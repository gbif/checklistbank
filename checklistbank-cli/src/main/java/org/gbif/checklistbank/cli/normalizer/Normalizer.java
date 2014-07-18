package org.gbif.checklistbank.cli.normalizer;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Strings;
import com.yammer.metrics.Gauge;
import com.yammer.metrics.Meter;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.jvm.MemoryUsageGaugeSet;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.TaxonProperties;
import org.gbif.checklistbank.neo.traverse.TaxonWalker;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Reads a good id based dwc archive and produces a neo4j graph from it.
 */
public class Normalizer {
    private static final Logger LOG = LoggerFactory.getLogger(Normalizer.class);

    private final NormalizerConfiguration cfg;
    private final UUID datasetKey;
    private final Map<String, UUID> constituents;
    private final File dwca;
    private final File storeDir;
    private GraphDatabaseService db;
    private final int batchSize;
    private final Meter insertMeter;
    private final Meter relationMeter;
    private final Meter metricsMeter;
    private final Gauge memory;
    private NormalizerStats stats;
    private NeoMapper mapper = NeoMapper.instance();
    private ExecutionEngine engine;

    public Normalizer(NormalizerConfiguration cfg, UUID datasetKey, MetricRegistry registry, Map<String, UUID> constituents) {
        this.constituents = constituents;
        this.cfg = cfg;
        this.datasetKey = datasetKey;
        this.insertMeter = registry.getMeters().get(NormalizerService.INSERT_METER);
        this.relationMeter = registry.getMeters().get(NormalizerService.RELATION_METER);
        this.metricsMeter = registry.getMeters().get(NormalizerService.METRICS_METER);
        this.memory = registry.getGauges().get(NormalizerService.HEAP_GAUGE);
        batchSize = cfg.neo.batchSize;
        storeDir = cfg.neo.neoDir(datasetKey);
        dwca = cfg.archiveDir(datasetKey);
    }

    /**
     * Uses an internal metrics registry to setup the normalizer
     */
    public static Normalizer build(NormalizerConfiguration cfg, UUID datasetKey, Map<String, UUID> constituents) {
        MetricRegistry registry = new MetricRegistry("normalizer");
        MemoryUsageGaugeSet mgs = new MemoryUsageGaugeSet();
        registry.registerAll(mgs);

        registry.meter(NormalizerService.INSERT_METER);
        registry.meter(NormalizerService.RELATION_METER);
        registry.meter(NormalizerService.METRICS_METER);

        return new Normalizer(cfg, datasetKey, registry, constituents);
    }

    public NormalizerStats run() throws NormalizationFailedException {
        stats = new NormalizerStats();
        // batch import uses its own batchdb
        batchInsertData();
        // create regular neo db for further processing
        setupDb();
        setupTaxonIdIndex();
        setupRelations();
        buildMetrics();
        tearDownDb();

        LOG.info("Normalization of {} finished. Database shut down.", datasetKey);
        return stats;
    }

    private void batchInsertData() throws NormalizationFailedException {
        NeoInserter inserter = new NeoInserter();
        inserter.insert(storeDir, dwca, stats, batchSize, insertMeter, constituents);
    }

    private void setupDb() {
        db = cfg.neo.newEmbeddedDb(datasetKey);
        engine = new ExecutionEngine(db, StringLogger.SYSTEM);
    }

    private void tearDownDb() {
        db.shutdown();
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
            LOG.debug("Deleted allAccepted {} relations", counter);
        }
    }

    /**
     * Creates implicit nodes and sets up relations between taxa.
     */
    private void setupRelations() {
        LOG.debug("Start processing relations ...");
        int counter = 0;
        int synonyms = 0;

        Transaction tx = db.beginTx();
        try {
            for (Node n : GlobalGraphOperations.at(db).getAllNodes()) {
                if (counter % batchSize == 0) {
                    tx.success();
                    tx.close();
                    LOG.debug("Relations processed for taxa: {}", counter);
                    //logMemory();
                    tx = db.beginTx();
                }

                final String taxonID = (String) n.getProperty(TaxonProperties.TAXON_ID, "");
                final String canonical = (String) n.getProperty(TaxonProperties.CANONICAL_NAME, "");
                final String sciname = (String) n.getProperty(TaxonProperties.SCIENTIFIC_NAME, "");

                boolean isSynonym = setupAcceptedRel(n, taxonID, sciname, canonical);
                setupParentRel(n, isSynonym, taxonID, sciname, canonical);
                setupBasionymRel(n, taxonID, sciname, canonical);

                counter++;
                if (isSynonym) {
                    synonyms++;
                }
                relationMeter.mark();
            }
            tx.success();
            stats.setSynonyms(synonyms);

        } finally {
            tx.close();
        }

        cleanupRelations();

        LOG.info("Relation setup completed, {} nodes processed", counter);
        LOG.info("Relation setup rate: {}", relationMeter.getMeanRate());
    }

    /**
     * Sanitizes relations and does the following cleanup:
     * <ul>
     *     <li>Relink synonym of synonyms to make sure synonyms always point to a direct accepted taxon.</li>
     *     <li>(Re)move parent relationship for synonyms.</li>
     *     <li>Break eternal classification loops at lowest rank</li>
     * </ul>
     */
    private void cleanupRelations() {
        LOG.debug("Cleanup relations ...");

        // cut synonym cycles
        try (Transaction tx = db.beginTx()){
            try {
                while (true) {
                    ExecutionResult result = engine.execute( "MATCH (s:TAXON)-[sr:SYNONYM_OF]->(x)-[:SYNONYM_OF*]->(s) RETURN sr LIMIT 1");
                    Relationship sr = (Relationship) IteratorUtil.first(result.columnAs("sr"));

                    Node syn = sr.getStartNode();
                    stats.getCycles().add((String) syn.getProperty(TaxonProperties.TAXON_ID, null));

                    Node acc = createTaxon(Origin.MISSING_ACCEPTED, NormalizerConstants.PLACEHOLDER_NAME, null, TaxonomicStatus.DOUBTFUL);
                    syn.createRelationshipTo(acc, RelType.SYNONYM_OF);
                    sr.delete();
                }
            } catch (NoSuchElementException e) {
                // all cycles removed
                tx.success();
            }
        }

        // relink synonym chain to single accepted
        try (Transaction tx = db.beginTx()){
            boolean more = true;
            while (more) {
                more = false;
                ExecutionResult result = engine.execute( "MATCH (s:TAXON)-[sr:SYNONYM_OF*]->(x)-[:SYNONYM_OF]->(t:TAXON) " +
                    "WHERE NOT (t)-[:SYNONYM_OF]->() " +
                    "RETURN sr, t LIMIT 1");
                for ( Map<String, Object> row : result ) {
                    more = true;
                    Node acc = (Node) row.get("t");
                    for (Relationship sr : (Collection<Relationship>) row.get("sr")) {
                        Node syn = sr.getStartNode();
                        syn.createRelationshipTo(acc, RelType.SYNONYM_OF);
                        sr.delete();
                    }
                }
            }
            tx.success();
        }

        LOG.info("Relations cleaned up, {} cycles detected", stats.getCycles().size());
    }

    private void buildMetrics() {
        ImportTaxonMetricsHandler handler = new ImportTaxonMetricsHandler();
        TaxonWalker.walkAccepted(db, handler, 10000, metricsMeter);
        stats.setDepth(handler.getMaxDepth());
        // do other final metrics
        try (Transaction tx = db.beginTx()) {
            stats.setRoots(IteratorUtil.count(GlobalGraphOperations.at(db).getAllNodesWithLabel(Labels.ROOT)));
        }

    }

    private void logMemory() {
        LOG.debug("Heap usage: {}", memory.getValue());
    }

    /**
     * Must deal with pro parte synonyms, i.e. a single synonym can have multiple accepted taxa!
     * @return true if it is a synonym of some type
     */
    private boolean setupAcceptedRel(Node n, String taxonID, String sciname, String canonical) {
        List<Node> accepted = Lists.newArrayList();
        if (n.hasProperty(DwcTerm.acceptedNameUsageID.simpleName())) {
            for (String id : listValue(n, DwcTerm.acceptedNameUsageID)) {
                if (id != null && !id.equals(taxonID)) {
                    Node a = nodeByTaxonId(id);
                    if (a == null) {
                        LOG.warn("acceptedNameUsageID {} not existing", id);
                    } else {
                        accepted.add(a);
                    }
                }
            }
        } else if (n.hasProperty(DwcTerm.acceptedNameUsage.simpleName())) {
            final String name = value(n, DwcTerm.acceptedNameUsage);
            if (name != null && !name.equals(sciname)) {
                Node a = nodeBySciname(name);
                if (a == null && !name.equals(canonical)) {
                    a = nodeByCanonical(name);
                    if (a == null) {
                        LOG.warn("acceptedNameUsage {} not existing", name);
                    }
                }
                if (a != null) {
                    accepted.add(a);
                }
            }
        }
        if (!accepted.isEmpty()) {
            for (Node a : accepted) {
                n.createRelationshipTo(a, RelType.SYNONYM_OF);
                n.addLabel(Labels.SYNONYM);
            }
            return true;
        }
        return false;
    }

    private void setupParentRel(Node n, boolean isSynonym, String taxonID, String sciname, String canonical) {
        Node parent = null;
        if (n.hasProperty(DwcTerm.parentNameUsageID.simpleName())) {
            final String id = value(n, DwcTerm.parentNameUsageID);
            if (id != null && !id.equals(taxonID)) {
                parent = nodeByTaxonId(id);
                if (parent == null) {
                    LOG.warn("parentNameUsageID {} not existing", id);
                }
            }
        } else if (n.hasProperty(DwcTerm.parentNameUsage.simpleName())) {
            final String name = value(n, DwcTerm.parentNameUsage);
            if (name != null && !name.equals(sciname)) {
                parent = nodeBySciname(name);
                if (parent == null && !name.equals(canonical)) {
                    parent = nodeByCanonical(name);
                    if (parent == null) {
                        LOG.warn("parentNameUsage {} not existing, materialize it", name);
                        parent = createTaxon(Origin.VERBATIM_PARENT, name, null, TaxonomicStatus.DOUBTFUL);
                        //TODO: link materialized parent into classification somehow???
                    }
                }
            }
        }

        if (parent != null) {
            parent.createRelationshipTo(n, RelType.PARENT_OF);
        } else if (!isSynonym) {
            n.addLabel(Labels.ROOT);
        }
    }

    private void setupBasionymRel(Node n, String taxonID, String sciname, String canonical) {
        Node basionym = null;
        if (n.hasProperty(DwcTerm.originalNameUsageID.simpleName())) {
            final String id = value(n, DwcTerm.originalNameUsageID);
            if (id != null && !id.equals(taxonID)) {
                basionym = nodeByTaxonId(id);
                if (basionym == null) {
                    LOG.warn("originalNameUsageID {} not existing", id);
                }
            }
        } else if (n.hasProperty(DwcTerm.originalNameUsage.simpleName())) {
            final String name = value(n, DwcTerm.originalNameUsage);
            if (name != null && !name.equals(sciname)) {
                basionym = nodeBySciname(name);
                if (basionym == null && !name.equals(canonical)) {
                    basionym = nodeByCanonical(name);
                    if (basionym == null) {
                        LOG.warn("originalNameUsage {} not existing, materialize it", name);
                        basionym = createTaxon(Origin.VERBATIM_BASIONYM, name, null, TaxonomicStatus.DOUBTFUL);
                        //TODO: link materialized parent into classification somehow???
                    }
                }
            }
        }
        if (basionym != null) {
            basionym.createRelationshipTo(n, RelType.BASIONYM_OF);
        }
    }

    private Node createTaxon(Origin origin, String sciname, Rank rank, TaxonomicStatus status) {
        NameUsage u = new NameUsage();
        u.setScientificName(sciname);
        u.setRank(rank);
        u.setOrigin(origin);
        u.setTaxonomicStatus(status);
        Node n = db.createNode(Labels.TAXON);
        mapper.store(n, u, false);

        stats.incRank(rank);
        stats.incOrigin(origin);
        return n;
    }


    private String value(Node n, Term term) {
        if (n.hasProperty(term.simpleName())) {
            return Strings.emptyToNull(((String) n.getProperty(term.simpleName(), null)).trim());
        }
        return null;
    }

    private String[] listValue(Node n, DwcTerm term) {
        if (n.hasProperty(term.simpleName())) {
            return (String[]) n.getProperty(term.simpleName(), new String[0]);
        }
        return new String[0];
    }

    /**
     * @return the single matching node with the taxonID or null
     */
    private Node nodeByTaxonId(String taxonID) {
        return IteratorUtil.singleOrNull(db.findNodesByLabelAndProperty(Labels.TAXON, TaxonProperties.TAXON_ID, taxonID));
    }

    /**
     * @return the single matching node with the canonical name or null
     */
    private Node nodeByCanonical(String canonical) {
        return IteratorUtil.singleOrNull(db.findNodesByLabelAndProperty(Labels.TAXON, TaxonProperties.CANONICAL_NAME, canonical));
    }

    /**
     * @return the single matching node with the scientific name or null
     */
    private Node nodeBySciname(String sciname) {
        return IteratorUtil.singleOrNull(db.findNodesByLabelAndProperty(Labels.TAXON, TaxonProperties.SCIENTIFIC_NAME, sciname));
    }
}
