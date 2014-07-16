package org.gbif.checklistbank.cli.normalizer;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.yammer.metrics.Gauge;
import com.yammer.metrics.Meter;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.jvm.MemoryUsageGaugeSet;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.traverse.TaxonWalker;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.StarRecord;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Reads a good id based dwc archive and produces a neo4j graph from it.
 */
public class Normalizer {
    private static final Logger LOG = LoggerFactory.getLogger(Normalizer.class);
    private static final Pattern NULL_PATTERN = Pattern.compile("^\\s*(\\\\N|\\\\?NULL)\\s*$");

    private final NormalizerConfiguration cfg;
    private final UUID datasetKey;
    private final File dwca;
    private final File storeDir;
    private GraphDatabaseService db;
    private boolean useCoreID = false;
    private final int batchSize;
    private final Meter insertMeter;
    private final Meter relationMeter;
    private final Meter metricsMeter;
    private final Gauge memory;

    public Normalizer(NormalizerConfiguration cfg, UUID datasetKey, Meter insertMeter, Meter relationMeter, Meter metricsMeter, Gauge memory) {
        this.cfg = cfg;
        this.datasetKey = datasetKey;
        this.insertMeter = insertMeter;
        this.relationMeter = relationMeter;
        this.metricsMeter = metricsMeter;
        this.memory = memory;
        batchSize = cfg.neo.batchSize;
        storeDir = cfg.neo.neoDir(datasetKey);
        dwca = cfg.archiveDir(datasetKey);
    }

    /**
     * Uses an internal metrics registry to setup the normalizer
     */
    public static Normalizer build(NormalizerConfiguration cfg, UUID datasetKey) {
        MetricRegistry registry = new MetricRegistry("normalizer");
        MemoryUsageGaugeSet mgs = new MemoryUsageGaugeSet();
        registry.registerAll(mgs);

        return new Normalizer(cfg, datasetKey,
            registry.meter("taxon inserts"),
            registry.meter("taxon relations"),
            registry.meter("taxon metrics"),
            (Gauge) mgs.getMetrics().get("heap.usage")
        );
    }

    public void run() throws NormalizationFailedException {
        batchInsertData();
        db = cfg.neo.newEmbeddedDb(datasetKey);
        setupTaxonIdIndex();
        setupRelations();
        buildMetrics();
        db.shutdown();
        LOG.info("Normalization of {} finished. Database shut down.", datasetKey);
    }


    private void batchInsertData() throws NormalizationFailedException {
        Archive arch = null;
        try {
            arch = ArchiveFactory.openArchive(dwca);
            if (!arch.getCore().hasTerm(DwcTerm.taxonID)) {
                LOG.warn("Using core ID for TAXON_ID_PROP");
                useCoreID = true;
            }
        } catch (IOException e) {
            throw new NormalizationFailedException("IOException opening archive " + dwca.getAbsolutePath(), e);
        }

        final BatchInserter inserter = BatchInserters.inserter(storeDir.getAbsolutePath());

        final BatchInserterIndexProvider indexProvider = new LuceneBatchInserterIndexProvider(inserter);
        final BatchInserterIndex taxonIdx = indexProvider.nodeIndex(DwcTerm.taxonID.simpleName(), MapUtil.stringMap("type", "exact"));
        taxonIdx.setCacheCapacity(DwcTerm.taxonID.simpleName(), 10000);

        final long startSort = System.currentTimeMillis();
        LOG.debug("Sorted archive in {} seconds", (System.currentTimeMillis() - startSort) / 1000);

        int counter = 0;
        for (StarRecord star : arch) {
            counter++;

            Record core = star.core();
            Map<String, Object> props = Maps.newHashMap();

            for (Term t : core.terms()) {
                String val = norm(core.value(t));
                if (val != null) {
                    props.put(t.simpleName(), val);
                }
            }
            // make sure this is last to override already put taxonID keys
            props.put(DwcTerm.taxonID.simpleName(), taxonID(core));
            // ... and into neo
            long node = inserter.createNode(props, Labels.TAXON);
            taxonIdx.add(node, props);

            insertMeter.mark();
            if (counter % (batchSize *10) == 0) {
                LOG.debug("insert: {}", counter);
            }
        }
        LOG.info("Data insert completed, {} nodes created", counter);
        LOG.info("Insert rate: {}", insertMeter.getMeanRate());

        indexProvider.shutdown();
        inserter.shutdown();
        LOG.info("Neo shutdown, data flushed to disk", counter);
    }

    private void setupTaxonIdIndex() {
        // setup unique index for TAXON_ID if not yet existing
        try (Transaction tx = db.beginTx()) {
            Schema schema = db.schema();
            if (IteratorUtil.count(schema.getIndexes(Labels.TAXON)) == 0) {
                LOG.debug("Create db indices ...");
                schema.constraintFor(Labels.TAXON).assertPropertyIsUnique(DwcTerm.taxonID.simpleName()).create();
                schema.indexFor(Labels.TAXON).on(DwcTerm.scientificName.simpleName()).create();
                tx.success();
            } else {
                LOG.debug("Neo indices existing already");
            }
        }
    }

    private void deleteAllRelations() {
        LOG.debug("Delete any existing relations");
        try (Transaction tx = db.beginTx()) {
            int counter = 0;
            for (Relationship rel : GlobalGraphOperations.at(db).getAllRelationships()) {
                rel.delete();
                counter++;
                if (counter % batchSize == 0) {
                    tx.success();
                    LOG.debug("Deleted {} relations", counter);
                }
            }
            tx.success();
            LOG.debug("Deleted all {} relations", counter);
        }
    }

    /**
     * Creates implicit nodes and sets up relations between taxa.
     */
    private void setupRelations() {
        LOG.debug("Start processing ...");
        long counter = 0;

        Transaction tx = db.beginTx();
        try {
            for (Node n : GlobalGraphOperations.at(db).getAllNodes()) {
                if (counter % batchSize == 0) {
                    tx.success();
                    tx.close();
                    LOG.debug("Relations processed for taxa: {}", counter);
                    logMemory();
                    tx = db.beginTx();
                }

                final String taxonID = (String) n.getProperty(DwcTerm.taxonID.simpleName());

                boolean isSynonym = setupAcceptedRel(n, taxonID);
                setupParentRel(n, isSynonym, taxonID);
                setupBasionymRel(n, taxonID);

                counter++;
                relationMeter.mark();
            }

        } finally {
            tx.close();
        }
        LOG.info("Import completed, {} nodes processed", counter);
        LOG.info("Relation setup rate: {}", relationMeter.getMeanRate());
    }

    private void buildMetrics() {
        ImportTaxonMetricsHandler handler = new ImportTaxonMetricsHandler();
        TaxonWalker.walkAll(db, handler, 10000, metricsMeter);
    }


    private void logMemory() {
        LOG.debug("Heap usage: {}", memory.getValue());
    }

    private boolean setupAcceptedRel(Node n, String taxonID) {
        final String aId = value(n, DwcTerm.acceptedNameUsageID);
        if (aId != null && !aId.equals(taxonID)) {
            Node accepted = nodeByTaxonId(aId);
            if (accepted != null) {
                n.createRelationshipTo(accepted, RelType.SYNONYM_OF);
                n.addLabel(Labels.SYNONYM);
            } else {
                LOG.warn("acceptedNameUsageID {} not existing", aId);
            }
            return true;
        }
        return false;
    }

    private void setupParentRel(Node n, boolean isSynonym, String taxonID) {
        final String pId = value(n, DwcTerm.parentNameUsageID);
        if (pId != null && !pId.equals(taxonID)) {
            Node parent = nodeByTaxonId(pId);
            if (parent != null) {
                parent.createRelationshipTo(n, RelType.PARENT_OF);
            } else {
                LOG.warn("parentNameUsageID {} not existing", pId);
            }
        } else if (!isSynonym) {
            n.addLabel(Labels.ROOT);
        }
    }

    private void setupBasionymRel(Node n, String taxonID) {
        final String id = value(n, DwcTerm.originalNameUsageID);
        if (id != null && !id.equals(taxonID)) {
            Node parent = nodeByTaxonId(id);
            if (parent != null) {
                parent.createRelationshipTo(n, RelType.BASIONYM_OF);
            } else {
                LOG.warn("originalNameUsageID {} not existing", id);
            }
        }
    }

    private String value(Node n, Term term) {
        return (String) n.getProperty(term.simpleName(), null);
    }

    private String taxonID(Record core) {
        if (useCoreID) {
            return norm(core.id());
        } else {
            return norm(core.value(DwcTerm.taxonID));
        }
    }

    private Node nodeByTaxonId(String taxonID) {
        return IteratorUtil.firstOrNull(db.findNodesByLabelAndProperty(Labels.TAXON, DwcTerm.taxonID.simpleName(), taxonID));
    }

    private String norm(String x) {
        if (Strings.isNullOrEmpty(x) || NULL_PATTERN.matcher(x).find()) {
            return null;
        }
        return x.trim();
    }

}
