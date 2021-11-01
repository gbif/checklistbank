package org.gbif.checklistbank.neo;

import com.codahale.metrics.MetricRegistry;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.model.GraphFormat;
import org.gbif.checklistbank.cli.model.NameUsageNode;
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.cli.model.UsageFacts;
import org.gbif.checklistbank.kryo.CliKryoFactory;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.neo.printer.*;
import org.gbif.checklistbank.neo.traverse.TreeWalker;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.utils.CleanupUtils;
import org.gbif.checklistbank.utils.SciNameNormalizer;
import org.gbif.nub.mapdb.MapDbObjectSerializer;
import org.gbif.nub.mapdb.MapDbUtils;
import org.mapdb.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.helpers.collection.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Stores usage data in 2 separate places for optimal performance and to reduce locking in long running transactions.
 * It uses neo to store the main relations and core properties often searched on, see NodeProperties
 * Pretty much all dao operations require an existing, open neo4j transaction to be managed outside of the dao which
 * only offers a beginTx() method.
 * For all the rest it uses a file persistent MapDB hashmap with kryo for quick serialization.
 */
public class UsageDao implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(UsageDao.class);
  private static final KryoPool KPOOL = new KryoPool.Builder(new CliKryoFactory())
      .softReferences()
      .build();

  private GraphDatabaseService neo;
  private final GraphDatabaseBuilder neoFactory;
  private final DB kvp;
  private final HTreeMap<Long, ParsedName> names;
  private final HTreeMap<Long, UsageFacts> facts;
  private final HTreeMap<Long, VerbatimNameUsage> verbatim;
  private final HTreeMap<Long, NameUsage> usages;
  private final HTreeMap<Long, UsageExtensions> extensions;
  private final HTreeMap<Long, SrcUsage> srcUsages;
  private final HTreeMap<Long, NubUsage> nubUsages;
  private final MetricRegistry registry;
  private final File neoDir;
  private final File kvpStore;
  private final Joiner remarkJoiner = Joiner.on("\n").skipNulls();

  /**
   * @param kvp
   * @param neoDir
   * @param neoFactory
   * @param registry
   */
  private UsageDao(DB kvp, File neoDir, @Nullable File kvpStore, GraphDatabaseBuilder neoFactory, @Nullable MetricRegistry registry) {
    try {
      this.neoFactory = neoFactory;
      this.neoDir = neoDir;
      this.kvpStore = kvpStore;
      this.kvp = kvp;
      this.registry = registry;

      names = createKvpMap("names", ParsedName.class, 128);
      facts = createKvpMap("facts", UsageFacts.class, 128);
      verbatim = createKvpMap("verbatim", VerbatimNameUsage.class, 512);
      usages = createKvpMap("usages", NameUsage.class, 256);
      extensions = createKvpMap("extensions", UsageExtensions.class, 512);
      srcUsages = createKvpMap("srcUsages", SrcUsage.class, 256);
      nubUsages = createKvpMap("nubUsages", NubUsage.class, 256);

      openNeo();
    } catch (Exception e) {
      LOG.error("Failed to initialize a new DAO", e);
      close();
      throw e;
    }
  }

  private <T> HTreeMap<Long, T> createKvpMap(String name, Class<T> clazz, int bufferSize) {
    return kvp.hashMap(name)
        .keySerializer(Serializer.LONG)
        .valueSerializer(new MapDbObjectSerializer(clazz, KPOOL, bufferSize))
        .createOrOpen();
  }

  /**
   * A memory based backend which is erased after the JVM exits.
   * Useful for short lived tests. Neo4j always persists some files which are cleaned up afterwards automatically
   *
   * @param mappedMemory used for the neo4j db
   */
  public static UsageDao temporaryDao(int mappedMemory, Integer shellPort) {
    LOG.debug("Create new in memory dao");
    DB kvp = DBMaker.memoryDB()
        .make();

    File storeDir = Files.createTempDir();
    NeoConfiguration cfg = new NeoConfiguration();
    cfg.mappedMemory = mappedMemory;
    if (shellPort != null) {
      cfg.bolt = true;
      cfg.port = shellPort;
    }
    GraphDatabaseBuilder builder = cfg.newEmbeddedDb(storeDir, false);
    CleanupUtils.registerCleanupHook(storeDir);

    return new UsageDao(kvp, storeDir, null, builder, new MetricRegistry());
  }

  /**
   * A memory based backend which is erased after the JVM exits.
   * Useful for short lived tests. Neo4j always persists some files which are cleaned up afterwards automatically
   *
   * @param mappedMemory used for the neo4j db
   */
  public static UsageDao temporaryDao(int mappedMemory) {
    return temporaryDao(mappedMemory, null);
  }

  /**
   * @return a dao of an existing neo db
   */
  public static UsageDao open(NeoConfiguration cfg, UUID datasetKey) {
    return persistentDao(cfg, datasetKey, null, false);
  }

  /**
   * @return creates a new, empty, persistent dao wiping any data that might have existed for that dataset
   */
  public static UsageDao create(NeoConfiguration cfg, UUID datasetKey) {
    return persistentDao(cfg, datasetKey, null, true);
  }

  /**
   * A backend that is stored in files inside the configured neo directory.
   *
   * @param eraseExisting if true erases any previous data files
   */
  public static UsageDao persistentDao(NeoConfiguration cfg, UUID datasetKey, MetricRegistry registry, boolean eraseExisting) {
    DB kvp = null;
    try {
      final File kvpF = cfg.kvp(datasetKey);
      final File storeDir = cfg.neoDir(datasetKey);
      if (eraseExisting) {
        LOG.debug("Remove existing data store");
        if (kvpF.exists()) {
          kvpF.delete();
        }
      }
      FileUtils.forceMkdir(kvpF.getParentFile());
      LOG.debug("Use KVP store {}", kvpF.getAbsolutePath());
      kvp = MapDbUtils.fileDB(kvpF).make();
      GraphDatabaseBuilder builder = cfg.newEmbeddedDb(storeDir, eraseExisting);
      return new UsageDao(kvp, storeDir, kvpF, builder, registry);

    } catch (Exception e) {
      if (kvp != null && !kvp.isClosed()) {
        kvp.close();
      }
      throw new IllegalStateException("Failed to init persistent DAO for " + datasetKey, e);
    }
  }

  public GraphDatabaseService getNeo() {
    return neo;
  }

  /**
   * Prints all neo4j node names out to a print stream, mainly for debugging.
   * This avoids (potentially erroneous) tree traversals missing some nodes.
   */
  public void logAll() throws Exception {
    Joiner joiner = Joiner.on(",").skipNulls();
    for (Node n : neo.getAllNodes()) {
      NubUsage u = readNub(n);
      LOG.info("{} {} [{} {}] {}", n.getId(), NeoProperties.getScientificName(n), (n.hasLabel(Labels.SYNONYM) ? TaxonomicStatus.SYNONYM.name() : TaxonomicStatus.ACCEPTED.name()).toLowerCase(), NeoProperties.getRank(n, null), u == null ? "" : joiner.join(u.issues));
    }
  }

  /**
   * Prints the entire neo4j tree out to a print stream, mainly for debugging.
   * Synonyms are marked with a prepended asterisk.
   */
  public void printTree(Writer writer, GraphFormat format) throws Exception {
    printTree(writer, format, true, null, null);
  }

  private static final Function<Node, String> getCanonical = new Function<Node, String>() {
    @Nullable
    @Override
    public String apply(@Nullable Node n) {
      return NeoProperties.getCanonicalName(n);
    }
  };

  private static final Function<Node, String> getScientific = new Function<Node, String>() {
    @Nullable
    @Override
    public String apply(@Nullable Node n) {
      return NeoProperties.getScientificName(n);
    }
  };

  /**
   * Prints the entire neo4j tree out to a print stream, mainly for debugging.
   * Synonyms are marked with a prepended asterisk.
   */
  public void printTree(Writer writer, GraphFormat format, final boolean fullNames, @Nullable Rank lowestRank, @Nullable Node root) throws Exception {
    TreePrinter printer;
    boolean includeProParte = false;
    switch (format) {
      case GML:
        printer = new GmlPrinter(writer, lowestRank, fullNames ? getScientific : getCanonical, true);
        break;

      case DOT:
        printer = new DotPrinter(writer, lowestRank, fullNames ? getScientific : getCanonical);
        break;

      case LIST:
        printer = new ListPrinter(writer, fullNames ? getScientific : getCanonical);
        break;

      case TAB:
        printer = new TabPrinter(writer, fullNames ? getScientific : getCanonical);
        break;

      case XML:
        printer = new XmlPrinter(writer);
        includeProParte = true;
        break;

      default:
        printer = new TxtPrinter(writer, fullNames ? getScientific : getCanonical);
        includeProParte = true;
        break;
    }
    TreeWalker.walkTree(getNeo(), includeProParte, root, lowestRank, null, printer);
    printer.close();
    writer.flush();
  }

  /**
   * Fully closes the dao leaving any potentially existing persistence files untouched.
   */
  @Override
  public void close() {
    try {
      if (kvp != null && !kvp.isClosed()) {
        kvp.close();
      }
    } catch (Exception e) {
      LOG.error("Failed to close kvp store {}", kvpStore.getAbsolutePath(), e);
    }
    closeNeo();
    System.gc(); // explicit GC, see https://github.com/gbif/checklistbank/issues/165
    LOG.info("Closed DAO for directory {}", neoDir.getAbsolutePath());
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
    LOG.debug("Starting embedded neo4j database from {}", neoDir.getAbsolutePath());
    neo = neoFactory.newGraphDatabase();
  }

  private void closeNeo() {
    try {
      if (neo != null) {
        neo.shutdown();
      }
    } catch (Exception e) {
      LOG.error("Failed to close neo4j {}", neoDir.getAbsolutePath(), e);
    }
  }

  /**
   * Shuts down the neo db is it was open and returns a neo inserter that uses a neo batch inserter under the hood.
   * Make sure you do not access any other dao methods until the batch inserter was closed properly!
   */
  public NeoInserter createBatchInserter(int batchSize) throws IOException {
    closeNeo();
    return NeoInserter.create(this, neoDir, batchSize, registry);
  }

  public Transaction beginTx() {
    return neo.beginTx();
  }

  /**
   * Finds nodes by their canonical name property.
   * Be careful when using this method on large graphs without a schema indexing the canonical name property!
   */
  public Collection<Node> findByName(String canonicalName) {
    return Iterators.asCollection(neo.findNodes(Labels.TAXON, NeoProperties.CANONICAL_NAME, canonicalName));
  }

  /**
   * @param canonicalName
   * @return th matching node, null or NoSuchElementException
   */
  public Node findByNameSingle(String canonicalName) {
    return Iterators.single(neo.findNodes(Labels.TAXON, NeoProperties.CANONICAL_NAME, canonicalName));
  }

  /**
   * @param scientificName
   * @return th matching node, null or NoSuchElementException
   */
  public Node findByScientificName(String scientificName) {
    return Iterators.single(neo.findNodes(Labels.TAXON, NeoProperties.SCIENTIFIC_NAME, scientificName));
  }

  /**
   * Creates a new neo node labeld as a taxon.
   *
   * @return the new & empty neo node
   */
  public Node createTaxon() {
    return neo.createNode(Labels.TAXON);
  }

  public UsageExtensions readExtensions(long key) {
    return extensions.get(key);
  }

  public void store(long key, UsageExtensions ext) {
    extensions.put(key, ext);
  }


  public UsageFacts readFacts(long key) {
    return facts.get(key);
  }

  public void store(long key, UsageFacts obj) {
    facts.put(key, obj);
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
    return readEnum(n, NeoProperties.RANK, Rank.class, Rank.UNRANKED);
  }

  private void updateNeo(Node n, NameUsage u) {
    if (n != null) {
      setProperty(n, NeoProperties.TAXON_ID, u.getTaxonID());
      setProperty(n, NeoProperties.SCIENTIFIC_NAME, u.getScientificName());
      setProperty(n, NeoProperties.CANONICAL_NAME, u.getCanonicalName());
      storeEnum(n, NeoProperties.RANK, u.getRank());
    }
  }

  public RankedName readRankedName(Node n) {
    RankedName rn = null;
    if (n != null) {
      rn = new RankedName();
      rn.node = n;
      rn.name = NeoProperties.getScientificName(n);
      rn.rank = readRank(n);
    }
    return rn;
  }

  public NubUsage readNub(Node n) {
    NubUsage nub = null;
    if (n != null) {
      nub = nubUsages.get(n.getId());
      if (nub != null) {
        nub.node = n;
      }
    }
    return nub;
  }

  public NubUsage readNub(long id) {
    return nubUsages.get(id);
  }

  public ParsedName readNubName(long id) {
    NubUsage nub = nubUsages.get(id);
    if (nub != null) {
      return nub.parsedName;
    }
    return null;
  }

  public ParsedName readName(long key) {
    return names.get(key);
  }

  public void store(long key, ParsedName pn) {
    names.put(key, pn);
  }

  /**
   * Reads a node into a name usage instance with keys being the node ids long values based on the neo relations.
   * The bulk of the usage data comes from the KVP store and neo properties are overlayed.
   */
  public NameUsage readUsage(Node n, boolean readRelations) {
    if (usages.containsKey(n.getId())) {
      NameUsage u = usages.get(n.getId());
      if (n.hasLabel(Labels.SYNONYM) && !u.isSynonym()) {
        u.setTaxonomicStatus(TaxonomicStatus.SYNONYM);
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
        u.setBasionym(NeoProperties.getScientificName(bas));
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
        u.setAccepted(NeoProperties.getScientificName(acc));
        // update synonym flag based on relations
        if (!u.isSynonym()) {
          u.setTaxonomicStatus(TaxonomicStatus.SYNONYM);
        }
      }
    } catch (RuntimeException e) {
      LOG.error("Unable to read accepted name relation for {} with node {}", u.getScientificName(), n.getId(), e);
      u.addIssue(NameUsageIssue.RELATIONSHIP_MISSING);
      NameUsageNode.addRemark(u, "Multiple accepted name relations");
    }

    try {
      // prefer the parent relationship of the accepted node if it exists
      Node p = getRelatedTaxon(acc == null ? n : acc, RelType.PARENT_OF, Direction.INCOMING);
      if (p != null) {
        u.setParentKey((int) p.getId());
        u.setParent(NeoProperties.getCanonicalName(p));
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
    Node n = createTaxon();
    // store usage in kvp store
    usages.put(n.getId(), u);
    // update neo with indexed properties
    updateNeo(n, u);
    return n;
  }

  /**
   * Stores the name usage instance overwriting anything that might have existed under that key.
   *
   * @param key       the node id to store under
   * @param u         the usage instance
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
    setProperty(nub.node, NeoProperties.CANONICAL_NAME, canonicalOrScientificName(nub.parsedName));
    setProperty(nub.node, NeoProperties.SCIENTIFIC_NAME, nub.parsedName.getScientificName());
    storeEnum(nub.node, NeoProperties.RANK, nub.rank);
  }

  /**
   * Updates a nub usage that does not have a neo node associated under the given node id.
   * Use this method with caution as it will not update neo4j and its indices!
   * Only use it for modified usages that have the properties stored in neo4j remain the same.
   */
  public void update(long nodeId, NubUsage nub) {
    nubUsages.put(nodeId, nub);
  }

  /**
   * @return the canonical name of a parsed name or the entire scientific name in case the canonical cannot be created (e.g. virus or hybrid names)
   */
  public static String canonicalOrScientificName(ParsedName pn) {
    if (pn.isParsed()) {
      String name = SciNameNormalizer.normalize(pn.canonicalName());
      if (!StringUtils.isBlank(name)) {
        return name;
      }
      LOG.error("Parsed {} name found with an empty canonical name string: {}", pn.getType(), pn.getScientificName());
    }
    return pn.getScientificName();
  }

  /**
   * Deletes kvp usage and neo node together with all its relations.
   */
  public void delete(NubUsage nub) {
    LOG.debug("Deleting node {} {}", nub.node.getId(), NeoProperties.getScientificName(nub.node));
    nubUsages.remove(nub.node.getId());
    // remove all relations
    for (Relationship rel : nub.node.getRelationships()) {
      rel.delete();
    }
    nub.node.delete();
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
    props.put(NeoProperties.TAXON_ID, taxonID);
    putIfNotNull(props, NeoProperties.SCIENTIFIC_NAME, u.getScientificName());
    putIfNotNull(props, NeoProperties.CANONICAL_NAME, u.getCanonicalName());
    putIfNotNull(props, NeoProperties.RANK, u.getRank());
    return props;
  }

  public ResourceIterator<Node> allTaxa() {
    return getNeo().findNodes(Labels.TAXON);
  }

  public ResourceIterable<Node> allNodes() {
    return getNeo().getAllNodes();
  }

  public ResourceIterator<Node> allRootTaxa() {
    return getNeo().findNodes(Labels.ROOT);
  }

  public ResourceIterator<Node> allFamilies() {
    return getNeo().findNodes(Labels.FAMILY);
  }

  public ResourceIterator<Node> allGenera() {
    return getNeo().findNodes(Labels.GENUS);
  }

  public ResourceIterator<Node> allSpecies() {
    return getNeo().findNodes(Labels.SPECIES);
  }

  public ResourceIterator<Node> allInfraSpecies() {
    return getNeo().findNodes(Labels.INFRASPECIES);
  }

  public ResourceIterator<Node> allAutonyms() {
    return getNeo().findNodes(Labels.AUTONYM);
  }

  public ResourceIterator<Node> allBasionyms() {
    return getNeo().findNodes(Labels.BASIONYM);
  }

  public ResourceIterator<Node> allSynonyms() {
    return getNeo().findNodes(Labels.SYNONYM);
  }

  public ResourceIterator<Node> allImplicitNames() {
    return getNeo().findNodes(Labels.IMPLICIT);
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
   *
   * @return the number of converted usages
   */
  public int convertNubUsages() {
    LOG.info("Converting all nub usages into name usages ...");
    int counter = 0;
    for (Map.Entry<Long, NubUsage> nub : nubUsages.getEntries()) {
      usages.put(nub.getKey(), convert(nub.getValue()));
      counter++;
    }
    LOG.info("Converted {} nub usages into name usages", counter);
    return counter;
  }

  public Iterable<Map.Entry<Long, NubUsage>> nubUsages() {
    return nubUsages.entrySet();
  }

  private NameUsage convert(NubUsage nub) {
    NameUsage u = new NameUsage();
    u.setKey(nub.usageKey);
    u.setConstituentKey(nub.datasetKey);
    //TODO: add a scientificNameID property to NameUsage
    //nub.scientificNameID
    u.setTaxonID("gbif:" + nub.usageKey);
    u.setScientificName(nub.parsedName.getScientificName());
    u.setCanonicalName(canonicalOrScientificName(nub.parsedName));
    u.setRank(nub.rank);
    u.setTaxonomicStatus(nub.status);
    u.setNomenclaturalStatus(nub.nomStatus);
    u.setPublishedIn(nub.publishedIn);
    u.setOrigin(nub.origin);
    if (!nub.sourceIds.isEmpty()) {
      u.setSourceTaxonKey(nub.sourceIds.getInt(0));
    } else if (nub.origin.equals(Origin.SOURCE)) {
      LOG.warn("Source usage without source id found {} {}", u.getKey(), u.getScientificName());
    }
    u.setRemarks(remarkJoiner.join(nub.remarks));
    u.setIssues(nub.issues);
    return u;
  }

  /**
   * Logs stats about the daos neo and kvp store.
   */
  public void logStats() {
    LOG.info("KVP store: " + kvpStore.getAbsolutePath());
    if (!kvp.isClosed()) {
      LOG.info("KVP facts: " + facts.size());
      LOG.info("KVP verbatim: " + verbatim.size());
      LOG.info("KVP usages: " + usages.size());
      LOG.info("KVP extensions: " + extensions.size());
      LOG.info("KVP srcUsages: " + srcUsages.size());
      LOG.info("KVP nubUsages: " + nubUsages.size());
    }
    LOG.info("neoDir: " + neoDir.getAbsolutePath());
    LOG.info("roots: " + Iterators.count(allRootTaxa()));
    LOG.info("families: " + Iterators.count(allFamilies()));
    LOG.info("genera: " + Iterators.count(allGenera()));
    LOG.info("basionyms: " + Iterators.count(allBasionyms()));
    LOG.info("synonyms: " + Iterators.count(allSynonyms()));
    LOG.info("all: " + Iterators.count(allTaxa()));
  }

  public void consistencyNubReport() {
    try (Transaction tx = neo.beginTx()) {
      int nCounter = 0;
      for (Node n : getNeo().getAllNodes()) {
        nCounter++;
        if (readNub(n) == null) {
          LOG.warn("Missing KVP nub usage for node {} {}", n.getId(), NeoProperties.getScientificName(n));
        }
      }
      LOG.info("Found {} neo4j nodes in total", nCounter);
    }

    try (Transaction tx = neo.beginTx()) {
      int kvpCounter = 0;
      for (long id : nubUsages.getKeys()) {
        kvpCounter++;
        try {
          neo.getNodeById(id);
        } catch (NotFoundException e) {
          NubUsage u = nubUsages.get(id);
          LOG.warn("Missing neo4j node for usage {}", u);
        }
      }
      LOG.info("Found {} kvp nub usages in total", kvpCounter);
    }
  }

  public ResourceIterator<Relationship> listAllRelationships(RelType type) {
    return neo.execute("match ()-[pp:" + type.name() + "]->() return pp").columnAs("pp");
  }

  /**
   * Compact mapdb memory usage.
   */
  public void compact() {
    kvp.commit();
    MapDbUtils.compact(names);
    MapDbUtils.compact(facts);
    MapDbUtils.compact(verbatim);
    MapDbUtils.compact(usages);
    MapDbUtils.compact(extensions);
    MapDbUtils.compact(srcUsages);
    MapDbUtils.compact(nubUsages);
  }
}
