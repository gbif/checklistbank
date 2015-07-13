package org.gbif.checklistbank.neo;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.kryo.ClbKryoFactory;
import org.gbif.checklistbank.kryo.MapDbObjectSerializer;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.neo.model.NameUsageNode;
import org.gbif.checklistbank.neo.model.RankedName;
import org.gbif.checklistbank.neo.model.UsageFacts;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

import com.beust.jcommander.internal.Maps;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.google.common.io.Files;
import com.yammer.metrics.MetricRegistry;
import org.apache.commons.io.FileUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores usage data in 2 separate places for optimal performance and to reduce locking in long running transactions.
 * It uses neo to store the main relations and core properties often searched on, see NodeProperties
 *
 * For all the rest it uses a file persistent MapDB hashmap with kryo for quick serialization.
 */
public class UsageDao {
    private static final Logger LOG = LoggerFactory.getLogger(UsageDao.class);

    private GraphDatabaseService neo;
    private final GraphDatabaseBuilder neoFactory;
    private final DB kvp;
    private final Map<Long, UsageFacts> facts;
    private final Map<Long, VerbatimNameUsage> verbatim;
    private final Map<Long, NameUsage> usages;
    private final Map<Long, UsageExtensions> extensions;
    private final Map<Long, SrcUsage> srcUsages;
    private final Map<Long, NubUsage> nubUsages;
    private final MetricRegistry registry;
    private final File neoDir;
    private final File kvpStore;
    private final KryoPool pool;

    /**
     * @param kvp
     * @param neoDir
     * @param neoFactory
     * @param registry
     */
    private UsageDao(DB kvp, File neoDir, @Nullable File kvpStore, GraphDatabaseBuilder neoFactory, MetricRegistry registry){
        this.neoFactory = neoFactory;
        this.neoDir = neoDir;
        this.kvpStore = kvpStore;
        this.kvp = kvp;
        this.registry = registry;

        pool = new KryoPool.Builder(new ClbKryoFactory())
                .softReferences()
                .build();
        facts = createKvpMap("facts", UsageFacts.class, 128);
        verbatim = createKvpMap("verbatim", VerbatimNameUsage.class, 512);
        usages = createKvpMap("usages", NameUsage.class, 256);
        extensions = createKvpMap("extensions", UsageExtensions.class, 512);
        srcUsages = createKvpMap("srcUsages", SrcUsage.class, 256);
        nubUsages = createKvpMap("nubUsages", NubUsage.class, 256);

        openNeo();
    }

    private <T> Map<Long, T> createKvpMap(String name, Class<T> clazz, int bufferSize) {
        return kvp.hashMapCreate(name)
                .keySerializer(Serializer.LONG)
                .valueSerializer(new MapDbObjectSerializer<T>(clazz, pool, bufferSize))
                .makeOrGet();
    }

    /**
     * A memory based backend which is erased after the JVM exits.
     * Useful for short lived tests. Neo4j always persists some files which are cleaned up afterwards automatically
     * @param mappedMemory used for the neo4j db
     */
    public static UsageDao temporaryDao(int mappedMemory) {
        LOG.info("Create new in memory dao");
        DB kvp = DBMaker.memoryDB()
                    .transactionDisable()
                    .make();

        File storeDir = Files.createTempDir();
        GraphDatabaseBuilder builder = newEmbeddedDb(storeDir, "strong", mappedMemory, false);
        registerCleanupHook(storeDir);

        return new UsageDao(kvp, storeDir, null, builder, new MetricRegistry("memory-dao"));
    }

    /**
     * A backend that is stored in files inside the configured neo directory.
     * @param eraseExisting if true erases any previous data files
     */
    public static UsageDao persistentDao(NeoConfiguration cfg, UUID datasetKey, MetricRegistry registry, boolean eraseExisting) {
        try {
            final File kvpF = cfg.kvp(datasetKey);
            final File storeDir = cfg.neoDir(datasetKey);
            if (eraseExisting) {
                if (kvpF.exists()) {
                    kvpF.delete();
                }
                if (storeDir.exists()) {
                    LOG.info("Remove existing data store");
                    FileUtils.deleteQuietly(storeDir);
                }
            }
            FileUtils.forceMkdir(kvpF.getParentFile());
            LOG.info("Use KVP store {}", kvpF.getAbsolutePath());
            DB kvp = DBMaker.fileDB(kvpF)
                        .fileMmapEnableIfSupported()
                        .transactionDisable()
                        .make();
            GraphDatabaseBuilder builder = newEmbeddedDb(storeDir, cfg.cacheType, cfg.mappedMemory, eraseExisting);
            return new UsageDao(kvp, storeDir, kvpF, builder, registry);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to init persistent DAO for " + datasetKey, e);
        }
    }

    /**
     * Creates a new embedded db in the neoRepository folder.
     * @param eraseExisting  if true deletes previously existing db
     */
    private static GraphDatabaseBuilder newEmbeddedDb(File storeDir, String cacheType, int mappedMemory, boolean eraseExisting) {
        if (eraseExisting && storeDir.exists()) {
            // erase previous db
            LOG.info("Removing previous neo4j database from {}", storeDir.getAbsolutePath());
            FileUtils.deleteQuietly(storeDir);
        }
        return new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(storeDir.getAbsolutePath())
                .setConfig(GraphDatabaseSettings.keep_logical_logs, "false")
                .setConfig(GraphDatabaseSettings.cache_type, cacheType)
                .setConfig(GraphDatabaseSettings.pagecache_memory, mb(mappedMemory));
    }

    private static void registerCleanupHook(final File f) {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
            if (f.exists()) {
                LOG.debug("Deleting file {}", f.getAbsolutePath());
                FileUtils.deleteQuietly(f);
            }
            }
        });
    }

    private static String mb(int memoryInMB) {
        return memoryInMB + "M";
    }

    public GraphDatabaseService getNeo() {
        return neo;
    }

    /**
     * Fully closes the dao leaving any potentially existing persistence files untouched.
     */
    public void close() {
        LOG.info("Closing dao ");
        if (!kvp.isClosed()){
            kvp.close();
        }
        closeNeo();
    }

    public void closeAndDelete() {
        close();
        if (kvpStore != null && kvpStore.exists()) {
            LOG.debug("Deleting kvp storage file {}", kvpStore.getAbsolutePath());
            FileUtils.deleteQuietly(kvpStore);
        }
        if (neoDir != null && neoDir.exists()) {
            LOG.debug("Deleting neo4j directory {}", neoDir.getAbsolutePath());
            FileUtils.deleteQuietly(neoDir);
        }
    }

    void openNeo() {
        LOG.info("Starting embedded neo4j database from {}", neoDir.getAbsolutePath());
        neo = neoFactory.newGraphDatabase();
    }

    private void closeNeo() {
        if (neo != null && neo.isAvailable(100)) {
            neo.shutdown();
        }
    }

    /**
     * Shuts down the neo db is it was open and returns a neo inserter that uses a neo batch inserter under the hood.
     * Make sure you do not access any other dao methods until the batch inserter was closed properly!
     */
    public NeoInserter createBatchInserter(int batchSize) {
        closeNeo();
        return NeoInserter.create(this, neoDir, batchSize, registry);
    }

    public Transaction beginTx() {
        return neo.beginTx();
    }

    /**
     * Creates a new neo node labeld as a taxon.
     * @return the new & empty neo node
     */
    public Node createTaxon() {
        return neo.createNode(Labels.TAXON);
    }

    public UsageExtensions readExtensions(long key) {
        return extensions.get(key);
    }

    public void store(long key, UsageExtensions ext) {
        this.extensions.put(key, ext);
    }


    public UsageFacts readFacts(long key) {
        return facts.get(key);
    }

    public void store(long key, UsageFacts obj) {
        this.facts.put(key, obj);
    }

    /**
     * Sets a node property and removes it in case the property value is null.
     */
    private static void setProperty(Node n, String property, Object value) {
        if (value == null) {
            n.removeProperty(property);
        } else {
            n.setProperty(property, value);
        }
    }

    private static <T> T readEnum(Node n, String property, Class<T> vocab, T defaultValue) {
        Object val = n.getProperty(property, null);
        if (val != null) {
            int idx = (Integer) val;
            return (T) vocab.getEnumConstants()[idx];
        }
        return defaultValue;
    }

    private static void storeEnum(Node n, String property, Enum value) {
        if (value == null) {
            n.removeProperty(property);
        } else {
            n.setProperty(property, value.ordinal());
        }
    }

    private Rank readRank(Node n) {
        return readEnum(n, NodeProperties.RANK, Rank.class, Rank.UNRANKED);
    }

    private void updateNeo(Node n, NameUsage u) {
        if (n != null){
            setProperty(n, NodeProperties.TAXON_ID, u.getTaxonID());
            setProperty(n, NodeProperties.SCIENTIFIC_NAME, u.getScientificName());
            setProperty(n, NodeProperties.CANONICAL_NAME, u.getCanonicalName());
            storeEnum(n, NodeProperties.RANK, u.getRank());
        }
    }

    private String readCanonicalName(Node n) {
        return (String) n.getProperty(NodeProperties.CANONICAL_NAME, null);
    }

    private String readScientificName(Node n) {
        return (String) n.getProperty(NodeProperties.SCIENTIFIC_NAME, null);
    }

    public RankedName readRankedName(Node n) {
        RankedName rn = null;
        if (n != null) {
            rn = new RankedName();
            rn.node = n;
            rn.name = readScientificName(n);
            rn.rank = readRank(n);
        }
        return rn;
    }

    public NubUsage readNub(Node n) {
        return nubUsages.get(n.getId());
    }

    /**
     * Reads a node into a name usage instance with keys being the node ids long values based on the neo relations.
     * The bulk of the usage data comes from the KVP store and neo properties are overlayed.
     */
    public NameUsage readUsage(Node n, boolean readRelations) {
        if (usages.containsKey(n.getId())) {
            NameUsage u = usages.get(n.getId());
            if (n.hasLabel(Labels.SYNONYM)) {
                u.setSynonym(true);
            }
            if (readRelations) {
                return readRelations(n, u);
            }
            return u;
        }
        return null;
    }

    private NameUsage readRelations(Node n, NameUsage u) {
        try {
            Node bas = getRelatedTaxon(n, RelType.BASIONYM_OF, Direction.INCOMING);
            if (bas != null) {
                u.setBasionymKey((int) bas.getId());
                u.setBasionym(readScientificName(bas));
            }
        } catch (RuntimeException e) {
            LOG.error("Unable to read basionym relation for {} with node {}", u.getScientificName(), n.getId());
            u.addIssue(NameUsageIssue.RELATIONSHIP_MISSING);
            NameUsageNode.addRemark(u, "Multiple original name relations");
        }

        Node acc = null;
        try {
            // pro parte synonym relations must have been flattened already...
            acc = getRelatedTaxon(n, RelType.SYNONYM_OF, Direction.OUTGOING);
            if (acc != null) {
                u.setAcceptedKey((int) acc.getId());
                u.setAccepted(readScientificName(acc));
                // update synonym flag based on relations
                u.setSynonym(true);
            }
        } catch (RuntimeException e) {
            LOG.error("Unable to read accepted name relation for {} with node {}", u.getScientificName(), n.getId());
            u.addIssue(NameUsageIssue.RELATIONSHIP_MISSING);
            NameUsageNode.addRemark(u, "Multiple accepted name relations");
        }

        try {
            // prefer the parent relationship of the accepted node if it exists
            Node p = getRelatedTaxon(acc == null ? n : acc, RelType.PARENT_OF, Direction.INCOMING);
            if (p != null) {
                u.setParentKey((int) p.getId());
                u.setParent(readScientificName(p));
            }
        } catch (RuntimeException e) {
            LOG.error("Unable to read parent relation for {} with node {}", u.getScientificName(), n.getId());
            u.addIssue(NameUsageIssue.RELATIONSHIP_MISSING);
            NameUsageNode.addRemark(u, "Multiple parent relations");
        }
        return u;
    }

    private Node getRelatedTaxon(Node n, RelType type, Direction dir) {
        Relationship rel = n.getSingleRelationship(type, dir);
        if (rel != null) {
            return rel.getOtherNode(n);
        }
        return null;
    }

    public Node create(NameUsage u) {
        Node n = createTaxon();;
        // store usage in kvp store
        usages.put(n.getId(), u);
        // update neo with indexed properties
        updateNeo(n, u);
        return n;
    }

    /**
     * Stores the name usage instance overwriting anything that might have existed under that key.
     * @param key the node id to store under
     * @param u the usage instance
     * @param updateNeo if true also update the neo4j properties used to populate NeoTaxon instances and the underlying lucene indices
     */
    public void store(long key, NameUsage u, boolean updateNeo) {
        usages.put(key, u);
        if (updateNeo) {
            // update neo with indexed properties
            updateNeo(neo.getNodeById(key), u);
        }
    }

    /**
     * Stores a modified name usage in the kvp store and optionally also updates the neo node properties if requested.
     * This method checks the modified flag on the NameUsageNode instance and does nothing if it is false.
     */
    public void store(NameUsageNode nn, boolean updateNeo) {
        if (nn.modified) {
            usages.put(nn.node.getId(), nn.usage);
            if (updateNeo) {
                // update neo with indexed properties
                updateNeo(nn.node, nn.usage);
            }
        }
    }

    /**
     * Stores verbatim usage using its key
     */
    public void store(long key, VerbatimNameUsage obj) {
        verbatim.put(key, obj);
    }
    
    public VerbatimNameUsage readVerbatim(long key) {
        return verbatim.get(key);
    }
    
    public void store(NubUsage nub) {
        nubUsages.put(nub.node.getId(), nub);
        // update neo node properties
        setProperty(nub.node, NodeProperties.CANONICAL_NAME, nub.parsedName.canonicalName());
        setProperty(nub.node, NodeProperties.SCIENTIFIC_NAME, nub.parsedName.canonicalNameComplete());
        storeEnum(nub.node, NodeProperties.RANK, nub.rank);
    }
    
    public SrcUsage readSourceUsage(Node n) {
        return srcUsages.get(n.getId());
    }
    
    public void storeSourceUsage(Node n, SrcUsage u) {
        srcUsages.put(n.getId(), u);
    }

    public Map<String, Object> neoProperties(String taxonID, NameUsage u, VerbatimNameUsage v) {
        Map<String, Object> props = Maps.newHashMap();
        // NeoTaxon properties
        props.put(NodeProperties.TAXON_ID, taxonID);
        putIfNotNull(props, NodeProperties.SCIENTIFIC_NAME, u.getScientificName());
        putIfNotNull(props, NodeProperties.CANONICAL_NAME, u.getCanonicalName());
        putIfNotNull(props, NodeProperties.RANK, u.getRank());
        return props;
    }

    public ResourceIterator<Node> allTaxa(){
        return getNeo().findNodes(Labels.TAXON);
    }

    private void putIfNotNull(Map<String, Object> props, String property, String value) {
        if (value != null) {
            props.put(property, value);
        }
    }

    private void putIfNotNull(Map<String, Object> props, String property, Enum value) {
        if (value != null) {
            props.put(property, value.ordinal());
        }
    }

    /**
     * Converts all nub usages present in the kvp store to full name usages
     * @return the number of converted usages
     */
    public int convertNubUsages() {
        LOG.info("Converting all nub usages into name usages ...");
        int counter = 0;
        for (Map.Entry<Long, NubUsage> nub : nubUsages.entrySet()) {
            NubUsage u = nub.getValue();
            usages.put(nub.getKey(), convert(nub.getKey().intValue(), u));
            counter++;
        }
        LOG.info("Converted {} nub usages into name usages", counter);
        return counter;
    }

    private NameUsage convert(int key, NubUsage nub) {
        NameUsage u = new NameUsage();
        u.setKey(key);
        u.setScientificName(nub.parsedName.canonicalNameComplete());
        u.setCanonicalName(nub.parsedName.canonicalName());
        u.setRank(nub.rank);
        u.setTaxonomicStatus(nub.status);
        u.setOrigin(nub.origin);
        u.setConstituentKey(nub.datasetKey);
        u.setNomenclaturalStatus(nub.nomStatus);
        u.setPublishedIn(nub.publishedIn);
        if (!nub.sourceIds.isEmpty()) {
            u.setSourceTaxonKey(nub.sourceIds.get(0));
        }
        return u;
    }

}
