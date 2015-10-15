package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.Metrics;
import org.gbif.checklistbank.cli.model.ClassificationKeys;
import org.gbif.checklistbank.cli.model.UsageFacts;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.neo.ImportDb;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.TaxonomicNodeIterator;
import org.gbif.checklistbank.service.UsageService;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.cursors.IntIntCursor;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.yammer.metrics.Meter;
import com.yammer.metrics.MetricRegistry;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importer that reads a neo database and syncs it with a postgres checklistbank db and solr index.
 * It understands pro parte synonym relations and creates multiple postgres usages for each accepted parent.
 */
public class Importer extends ImportDb implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Importer.class);
    private final static int SELF_ID = -1;
    private final Meter syncMeter;
    private int syncCounter;
    private int delCounter;
    private final DatasetImportServiceCombined importService;
    private final NameUsageService nameUsageService;
    private final UsageService usageService;
    // neo internal ids to clb usage keys
    private IntIntMap clbKeys = new IntIntHashMap();
    // map based around internal neo4j node ids:
    private IntObjectMap<Integer[]> postKeys = new IntObjectHashMap<Integer[]>();
    // map of existing pro parte synonym clb usage keys to their accepted taxon given as neo node id to be updated at the very end
    private IntIntMap postProParteKeys = new IntIntHashMap();
    private int maxExistingNubKey = -1;

    private enum KeyType {PARENT, ACCEPTED, BASIONYM, CLASSIFICATION}

    ;
    private final int keyTypeSize = KeyType.values().length;

    private Importer(UUID datasetKey, UsageDao dao, MetricRegistry registry,
                     DatasetImportServiceCombined importService, NameUsageService nameUsageService, UsageService usageService) {
        super(datasetKey, dao);
        this.importService = importService;
        this.nameUsageService = nameUsageService;
        this.usageService = usageService;
        this.syncMeter = registry.meter(Metrics.SYNC_METER);
    }

    /**
     * @param usageService only needed if you gonna sync the backbone dataset. Tests can usually just pass in null!
     */
    public static Importer create(ImporterConfiguration cfg, UUID datasetKey, MetricRegistry registry,
                                  DatasetImportServiceCombined importService, NameUsageService nameUsageService, UsageService usageService) {
        return new Importer(datasetKey,
                UsageDao.persistentDao(cfg.neo, datasetKey, true, registry, false),
                registry, importService, nameUsageService, usageService);
    }

    public void run() {
        LOG.info("Start importing checklist {}", datasetKey);
        try {
            if (LOG.isDebugEnabled()) {
                dao.printTree();
            }
            syncDataset();
            LOG.info("Importing of {} succeeded.", datasetKey);
        } finally {
            dao.close();
            LOG.info("Neo database {} shut down.", datasetKey);
        }
    }

    /**
     * Iterates over all accepted taxa in taxonomical order including all synonyms and syncs the usage individually
     * with Checklist Bank Postgres. As basionym relations can crosslink basically any record we first set the basionym
     * key to null and update just those keys in a second iteration. Most usages will not have a basionymKey, so
     * performance should only be badly impacted in rare cases.
     *
     * @throws EmptyImportException if no records at all have been imported
     */
    private void syncDataset() throws EmptyImportException {
        if (datasetKey.equals(Constants.NUB_DATASET_KEY)) {
            // remember the current highest nub key so we know if incoming ones are inserts or updates
            Integer high = usageService.maxUsageKey(Constants.NUB_DATASET_KEY);
            maxExistingNubKey = high == null ? -1 : high;
            LOG.info("Sync GBIF backbone. Current max nub usageKey={}", maxExistingNubKey);
        }
        // we keep the very first usage key to retrieve the exact last modified timestamp from the database
        // in order to avoid clock differences between machines and threads.
        int firstUsageKey = -1;

        try (Transaction tx = dao.beginTx()) {
            // returns all nodes, accepted and synonyms
            for (Node n : TaxonomicNodeIterator.all(dao.getNeo())) {
                try {
                    final int nodeId = (int) n.getId();
                    VerbatimNameUsage verbatim = dao.readVerbatim(nodeId);
                    UsageFacts facts = dao.readFacts(nodeId);
                    if (facts == null) {
                        facts = new UsageFacts();
                        facts.metrics = new NameUsageMetrics();
                    }
                    NameUsage u = buildClbNameUsage(n, facts.classification);
                    List<Integer> parents = buildClbParents(n);
                    UsageExtensions ext = dao.readExtensions(nodeId);

                    // do we have pro parte relations? we need to duplicate the synonym for each additional accepted taxon to fit the postgres db model
                    final int usageKey;
                    if (n.hasRelationship(Direction.OUTGOING, RelType.PROPARTE_SYNONYM_OF)) {
                        u.setTaxonomicStatus(TaxonomicStatus.PROPARTE_SYNONYM);
                        u.setProParteKey(SELF_ID);
                        usageKey = syncUsage(u, parents, verbatim, facts.metrics, ext);
                        LOG.debug("First synced pro parte usage {}", usageKey);
                        // now insert the other pro parte records
                        u.setProParteKey(usageKey);
                        u.setOrigin(Origin.PROPARTE);
                        u.setTaxonID(null); // if we keep the original id we will do an update, not an insert
                        for (Relationship rel : n.getRelationships(RelType.PROPARTE_SYNONYM_OF, Direction.OUTGOING)) {
                            Node accN = rel.getEndNode();
                            int accNodeId = (int) accN.getId();
                            boolean existingKey = false;
                            // not all accepted usages might already exist in postgres, only use the key if they exist
                            if (clbKeys.containsKey(accNodeId)) {
                                u.setAcceptedKey(clbKeys.get(accNodeId));
                                existingKey = true;
                            }
                            // pro parte synonyms keep their id in the relation, read it
                            // http://dev.gbif.org/issues/browse/POR-2872
                            u.setKey( (Integer) n.getProperty(NeoProperties.USAGE_KEY, null));
                            // sync the extra usage
                            int ppid = syncUsage(u, parents, verbatim, facts.metrics, ext);
                            if (!existingKey) {
                                // in case the accepted usage does not yet exist remember the relation and update the usage at the very end
                                postProParteKeys.put(ppid, accNodeId);
                            }
                            syncCounter++;
                        }
                    } else {
                        usageKey = syncUsage(u, parents, verbatim, facts.metrics, ext);
                    }
                    // keep map of node ids to clb usage keys
                    clbKeys.put(nodeId, usageKey);
                    if (firstUsageKey < 0) {
                        firstUsageKey = usageKey;
                        LOG.info("First synced usage key for dataset {} is {}", datasetKey, firstUsageKey);
                    }
                    syncMeter.mark();
                    syncCounter++;
                    if (syncCounter % 100000 == 0) {
                        LOG.info("Synced {} usages from dataset {}, latest usage key={}", syncCounter, datasetKey, usageKey);
                    } else if (syncCounter % 10000 == 0) {
                        LOG.debug("Synced {} usages from dataset {}, latest usage key={}", syncCounter, datasetKey, usageKey);
                    }

                } catch (Throwable e) {
                    String id;
                    if (n.hasProperty(NeoProperties.TAXON_ID)) {
                        id = String.format("taxonID '%s'", n.getProperty(NeoProperties.TAXON_ID));
                    } else {
                        id = String.format("nodeID %s", n.getId());
                    }
                    LOG.error("Failed to sync {} {} from dataset {}", n.getProperty(NeoProperties.SCIENTIFIC_NAME, ""), id, datasetKey);
                    LOG.error("Aborting sync of dataset {}", datasetKey);
                    throw e;
                }
            }
        }

        // finally update foreign keys that did not exist during initial inserts
        if (!postKeys.isEmpty()) {
            LOG.info("Updating foreign keys for {} usages from dataset {}", postKeys.size(), datasetKey);
            for (IntObjectCursor<Integer[]> c : postKeys) {
                //TODO: remove this try section as it should NEVER happen in good, production code!!!
                // If it does the import should fail - this is only for nub build tests
                try {
                    // update usage by usage doing both potential updates in one statement
                    Optional<Integer> parentKey = Optional.fromNullable(c.value[KeyType.ACCEPTED.ordinal()])
                                              .or(Optional.fromNullable(c.value[KeyType.PARENT.ordinal()]));
                    importService.updateForeignKeys(clbKey(c.key),
                            clbKey(parentKey.orNull()),
                            clbKey(c.value[KeyType.BASIONYM.ordinal()])
                    );
                } catch (IllegalStateException e) {
                    LOG.error("CLB ID integrity problem", e);
                }
            }
        }
        if (!postProParteKeys.isEmpty()) {
            LOG.info("Updating foreign keys for {} pro parte usages from dataset {}", postProParteKeys.size(), datasetKey);
            for (IntIntCursor c : postProParteKeys) {
                //TODO: remove this try section as it should NEVER happen in good, production code!!!
                try {
                    // update usage by usage doing both potential updates in one statement
                    importService.updateForeignKeys(c.key, clbKey(c.value), null);
                } catch (IllegalStateException e) {
                    LOG.error("CLB ID integrity problem", e);
                }
            }
        }
        
        // remove old usages
        if (firstUsageKey < 0) {
            LOG.warn("No records imported for dataset {}. Keep all existing data!", datasetKey);
            throw new EmptyImportException(datasetKey, "No records imported for dataset " + datasetKey);

        } else {
            NameUsage first = nameUsageService.get(firstUsageKey, null);
            if (first == null) {
                LOG.error("First synced name usage with id {} not found", firstUsageKey);
                throw new EmptyImportException(datasetKey, "Error importing name usages for dataset " + datasetKey);
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(first.getLastInterpreted());
            // use 2 seconds before first insert/update as the threshold to remove records
            cal.add(Calendar.SECOND, -2);
            delCounter = importService.deleteOldUsages(datasetKey, cal.getTime());
        }
    }

    private int syncUsage(NameUsage u, List<Integer> parents, VerbatimNameUsage verbatim, NameUsageMetrics metrics, UsageExtensions ext) {
        if (datasetKey.equals(Constants.NUB_DATASET_KEY)) {
            // for nub builts we generate the usageKey in code already. Both for inserts and updates.
            // just for pro parte usages we use the sequence generator!
            return importService.syncUsage(u.getKey() == null || u.getKey() > maxExistingNubKey, u, parents, verbatim, metrics, ext);
        } else {
            return importService.syncUsage(false, u, parents, verbatim, metrics, ext);
        }
    }

    /**
     * @return list of parental clb usage keys
     */
    private List<Integer> buildClbParents(Node n) {
        // we copy the transformed, short list as it is still backed by some neo transaction
        return Lists.newArrayList(Lists.transform(
                        IteratorUtil.asList(n.getRelationships(RelType.PARENT_OF, Direction.INCOMING)),
                        new Function<Relationship, Integer>() {
                            @Override
                            public Integer apply(Relationship rel) {
                                return rel != null ? clbKey((int) rel.getStartNode().getId()) : null;
                            }
                        })
        );
    }

    /**
     * Maps a neo node id to an already created clb postgres id.
     * If the mapping does not exist an IllegalStateException is thrown.
     */
    private Integer clbKey(Integer nodeId) {
        if (nodeId == null) {
            return null;
        }
        if (clbKeys.containsKey(nodeId)) {
            return clbKeys.get(nodeId);
        } else {
            throw new IllegalStateException("NodeId not in CLB yet: " + nodeId);
        }
    }

    /**
     * Maps a neo node id of a foreign key to an already created clb postgres id.
     * If the requested nodeID actually refers to the current node id, then -1 will be returned to indicate to the mybatis
     * mapper that it should use the newly generated sequence value.
     *
     * @param nodeId the node id casted from long that represents the currently processed name usage record
     * @param nodeFk the foreign key to the node id we wanna setup the relation to
     */
    private Integer clbForeignKey(long nodeId, Integer nodeFk, KeyType type) {
        if (nodeFk == null) return null;

        if (clbKeys.containsKey(nodeFk)) {
            // already imported the node and we know the clb key
            return clbKeys.get(nodeFk);
        } else if (nodeId == (long) nodeFk) {
            // tell postgres to use the newly generated key of the inserted record
            return SELF_ID;
        } else if (KeyType.CLASSIFICATION == type) {
            // should not happen as we process the usages in a taxonomic hierarchy from top down.
            // if you see this it looks like the normalizer did a bad job somewhere
            throw new IllegalStateException("Higher classification NodeId not in CLB yet: " + nodeFk);
        } else {
            // remember non classification keys for update after all records have been synced once
            int nid = (int) nodeId;
            if (postKeys.containsKey(nid)) {
                postKeys.get(nid)[type.ordinal()] = nodeFk;
            } else {
                Integer[] keys = new Integer[keyTypeSize];
                keys[type.ordinal()] = nodeFk;
                postKeys.put(nid, keys);
            }
            return null;
        }
    }

    /**
     * Reads the full name usage from neo and updates all foreign keys to use CLB usage keys.
     */
    @VisibleForTesting
    protected NameUsage buildClbNameUsage(Node n, @Nullable ClassificationKeys classification) {
        // this is using neo4j internal node ids as keys:
        NameUsage u = dao.readUsage(n, true);
        Preconditions.checkNotNull(u, "Node %s not found in kvp store", n.getId());
        if (classification != null) {
            ClassificationUtils.copyLinneanClassificationKeys(classification, u);
        }
        if (n.hasLabel(Labels.SYNONYM)) {
            u.setSynonym(true);
            u.setAcceptedKey(clbForeignKey(n.getId(), u.getAcceptedKey(), KeyType.ACCEPTED));
        } else {
            u.setSynonym(false);
            u.setParentKey(clbForeignKey(n.getId(), u.getParentKey(), KeyType.PARENT));
        }
        u.setBasionymKey(clbForeignKey(n.getId(), u.getBasionymKey(), KeyType.BASIONYM));
        for (Rank r : Rank.DWC_RANKS) {
            try {
                ClassificationUtils.setHigherRankKey(u, r, clbForeignKey(n.getId(), u.getHigherRankKey(r), KeyType.CLASSIFICATION));
            } catch (IllegalStateException e) {
                LOG.error("{} (nodeID={}) has unprocessed {} reference to nodeId {}", n.getProperty(NeoProperties.SCIENTIFIC_NAME, "no name"), n.getId(), r, u.getHigherRankKey(r));
                throw e;
            }
        }
        u.setDatasetKey(datasetKey);
        return u;
    }

    public int getSyncCounter() {
        return syncCounter;
    }

    public int getDelCounter() {
        return delCounter;
    }
}
