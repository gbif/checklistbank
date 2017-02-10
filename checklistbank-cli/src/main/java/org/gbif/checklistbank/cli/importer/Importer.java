package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.model.UsageFacts;
import org.gbif.checklistbank.kryo.CliKryoFactory;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.model.UsageForeignKeys;
import org.gbif.checklistbank.neo.ImportDb;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.ChunkingEvaluator;
import org.gbif.checklistbank.neo.traverse.MultiRootNodeIterator;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.neo.traverse.TreeIterablesSorted;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ImporterCallback;
import org.gbif.checklistbank.service.UsageService;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
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
public class Importer extends ImportDb implements Runnable, ImporterCallback {

  private static final Logger LOG = LoggerFactory.getLogger(Importer.class);
  private final static int SELF_ID = -1;
  private final ImporterConfiguration cfg;
  private int syncCounterMain;
  private int syncCounterBatches;
  private int syncCounterProParte;
  private int delCounter;
  private final DatasetImportService sqlService;
  private final DatasetImportService solrService;
  private final NameUsageService nameUsageService;
  private final UsageService usageService;
  // neo internal ids to clb usage keys
  private ConcurrentHashMap<Integer, Integer> clbKeys = new ConcurrentHashMap<Integer, Integer>();
  // map based around internal neo4j node ids:
  private ConcurrentHashMap<Integer, UsageForeignKeys> postKeys = new ConcurrentHashMap<Integer, UsageForeignKeys>();
  // list of pro parte synonym neo node ids
  private Set<Long> proParteNodes = Sets.newHashSet();
  private int maxExistingNubKey = -1;
  private volatile int firstUsageKey = -1;
  private Future<List<NameUsage>> proParteFuture;
  private Queue<Future<List<Integer>>> usageFutures = new ConcurrentLinkedQueue<Future<List<Integer>>>();
  private Queue<Future<?>> otherFutures = new ConcurrentLinkedQueue<Future<?>>();

  private final KryoPool kryoPool = new KryoPool.Builder(new CliKryoFactory()).build();

  private enum KeyType {PARENT, ACCEPTED, BASIONYM, CLASSIFICATION}

  @Inject
  private Importer(UUID datasetKey, UsageDao dao,
                   NameUsageService nameUsageService, UsageService usageService,
                   DatasetImportService sqlService, DatasetImportService solrService,
                   ImporterConfiguration cfg) {
    super(datasetKey, dao);
    this.cfg = cfg;
    this.nameUsageService = nameUsageService;
    this.usageService = usageService;
    this.sqlService = sqlService;
    this.solrService = solrService;
  }

  /**
   * @param usageService only needed if you gonna sync the backbone dataset. Tests can usually just pass in null!
   */
  public static Importer create(ImporterConfiguration cfg, UUID datasetKey,
                                NameUsageService nameUsageService, UsageService usageService,
                                DatasetImportService sqlService, DatasetImportService solrService) {
    return new Importer(datasetKey,
        UsageDao.persistentDao(cfg.neo, datasetKey, true, null, false),
        nameUsageService, usageService,
        sqlService, solrService,
        cfg);
  }

  public void run() {
    LOG.info("Start importing checklist");
    try {
      syncDataset();
      LOG.info("Waiting for threads to finish {} sql and {} solr jobs", usageFutures.size(), otherFutures.size());
      awaitUsageFutures();
      awaitProParteFuture();
      // wait for extensions and solr jobs to finish
      awaitOtherFutures();
      LOG.info("Importing succeeded. {} main, {} subtree chunk and {} pro parte usages synced", syncCounterMain, syncCounterBatches, syncCounterProParte);

    } catch (InterruptedException e) {
      Throwables.propagate(e);
      LOG.error("Job interrupted, data is likely to be inconsistent.");
      Thread.currentThread().interrupt();

    } catch (ExecutionException e) {
      LOG.error("Error executing job", e.getCause());
      Throwables.propagate(e);

    } finally {
      LOG.debug("Shutting down graph database");
      dao.close();
      LOG.info("Neo database shut down.");
    }
  }

  /**
   * Iterates over all accepted taxa in taxonomical order including all synonyms and syncs the usage individually
   * with Checklist Bank Postgres. As basionym relations can crosslink basically any record we first set the basionym
   * key to null and update just those keys in a second iteration. Most usages will not have a basionymKey, so
   * performance should only be badly impacted in rare cases.
   *
   * The taxonomic tree is traversed and whenever possible subtrees are processed in separate batches.
   * The main traversal will happen more or less syncronously as it is required to exist before the subtrees can be synced.
   *
   * @throws EmptyImportException if no records at all have been imported
   */
  private void syncDataset() throws EmptyImportException, ExecutionException, InterruptedException {
    if (datasetKey.equals(Constants.NUB_DATASET_KEY)) {
      // remember the current highest nub key so we know if incoming ones are inserts or updates
      Integer high = usageService.maxUsageKey(Constants.NUB_DATASET_KEY);
      maxExistingNubKey = high == null ? -1 : high;
      LOG.info("Sync GBIF backbone. Current max nub usageKey={}", maxExistingNubKey);
    }
    // we keep the very first usage key to retrieve the exact last modified timestamp from the database
    // in order to avoid clock differences between machines and threads.
    firstUsageKey = -1;
    int chunks = 0;

    // traverse the tree
    try (Transaction tx = dao.beginTx()) {
      LOG.info("Chunking imports into slices of {} to {}", cfg.chunkMinSize, cfg.chunkSize);
      ChunkingEvaluator chunkingEvaluator = new ChunkingEvaluator(dao, cfg.chunkMinSize, cfg.chunkSize);
      List<Integer> batch = Lists.newArrayList();
      for (Node n : MultiRootNodeIterator.create(TreeIterablesSorted.findRoot(dao.getNeo()), Traversals.TREE_WITHOUT_PRO_PARTE.evaluator(chunkingEvaluator))) {
        if (chunkingEvaluator.isChunk(n.getId())) {
          LOG.debug("chunk node {} found", n.getId());
          Future<List<Integer>> f = null;
          if (!batch.isEmpty()) {
            LOG.debug("submit {} main nodes for concurrent syncing starting with node {}", batch.size(), batch.get(0));
            f = sqlService.sync(datasetKey, this, batch);
          }
          // while main nodes sync we can read in the new subtree already
          batch = subtreeBatch(n);
          chunks++;
          syncCounterBatches = syncCounterBatches + batch.size();
          // wait for main future to finish and submit solr update ...
          if (f != null) {
            otherFutures.add(solrService.sync(datasetKey,this, f.get()));
            LOG.debug("main nodes synced. Submit solr update");
          }
          // main nodes are in postgres. Now we can submit the sync task for the subtree
          LOG.debug("submit subtree chunk with {} usages starting with {}", batch.size(), n);
          usageFutures.add(sqlService.sync(datasetKey, this, batch));
          // reset main batch for new usages
          batch = Lists.newArrayList();
          clearFinishedUsageTasks();

        } else {
          // add to main batch
          batch.add((int)n.getId());
          if (isProParteNode(n)) {
            proParteNodes.add(n.getId());
          }
          syncCounterMain++;
        }
      }
      if (!batch.isEmpty()) {
        LOG.debug("submit final {} main nodes for concurrent syncing", batch.size());
        usageFutures.add(sqlService.sync(datasetKey,this, batch));
      }
    }

    // wait for main sql usage imports to be done so we dont break foreign key constraints
    LOG.info("Wait for usage import tasks to finish.");
    awaitUsageFutures();
    LOG.info("Core usage import completed. {} chunk jobs synced with {} main usages and {} subtree batch usages usages.", chunks, syncCounterMain, syncCounterBatches);
    if (clbKeys.size() != syncCounterMain + syncCounterBatches) {
      LOG.warn("{} clb usage keys known for {} neo nodes ({} main, {} chunk). Expecting \"NodeId not in CLB exceptions\" ...", clbKeys.size(), syncCounterMain+syncCounterBatches, syncCounterMain, syncCounterBatches);
    }

    // finally update foreign keys that did not exist during initial inserts
    updateForeignKeys();

    // finally import extra pro parte usages
    syncProParte();

    // make sure we have imported at least one record
    if (firstUsageKey < 0) {
      LOG.warn("No records imported. Keep all existing data!");
      throw new EmptyImportException(datasetKey, "No records imported for dataset " + datasetKey);
    }

    // remove old usages
    deleteOldUsages();
  }

  private void updateForeignKeys() {
    if (!postKeys.isEmpty()) {
      // update neo ids to clb usage keys
      LOG.info("Updating foreign keys for {} usages", postKeys.size());
      for (UsageForeignKeys fk : postKeys.values()) {
        fk.setUsageKey(clbKey(fk.getUsageKey()));
        fk.setParentKey(clbKey(fk.getParentKey()));
        fk.setBasionymKey(clbKey(fk.getBasionymKey()));
      }
      List<UsageForeignKeys> fks = ImmutableList.copyOf(postKeys.values());
      sqlService.updateForeignKeys(datasetKey, fks);
      solrService.updateForeignKeys(datasetKey, fks);
    }
  }

  private void syncProParte() {
    if (!proParteNodes.isEmpty()) {
      LOG.info("Syncing {} pro parte usages", proParteNodes.size());
      for (List<Long> ids : Iterables.partition(proParteNodes, cfg.chunkSize)) {
        List<NameUsage> usages = Lists.newArrayList();
        List<ParsedName> names = Lists.newArrayList();
        try (Transaction tx = dao.getNeo().beginTx()) {
          for (Long id : ids) {
            Node n = dao.getNeo().getNodeById(id);
            NameUsage primary = readUsage(n);
            ParsedName pn = readName(id);
            // modify as a template for all cloned pro parte usages
            primary.setProParteKey(primary.getKey());
            primary.setOrigin(Origin.PROPARTE);
            primary.setTaxonID(null); // if we keep the original id we will do an update, not an insert
            primary.setParentKey(null);
            for (Relationship rel : n.getRelationships(RelType.PROPARTE_SYNONYM_OF, Direction.OUTGOING)) {
              // pro parte synonyms keep their id in the relation, read it
              // http://dev.gbif.org/issues/browse/POR-2872
              NameUsage u = clone(primary);
              u.setKey( (Integer) rel.getProperty(NeoProperties.USAGE_KEY, null));
              Node accN = rel.getEndNode();
              // all nodes should be synced by now, so clb keys must be known
              u.setAcceptedKey(clbKeys.get((int) accN.getId()));
              // use accepted taxon classification for this synonym record
              applyClbClassification(u, accN.getId());
              usages.add(u);
              names.add(pn);
            }
          }
        }
        // submit sync job
        syncCounterProParte = syncCounterProParte + usages.size();
        proParteFuture = sqlService.sync(datasetKey, this, usages, names);
      }
    }
  }

  /**
   * Applies the classification from another node, transforming the neo node ids into existing clb usage keys
   * @param u
   * @param classificationNodeId
   */
  private void applyClbClassification(NameUsage u, long classificationNodeId) {
    UsageFacts facts = dao.readFacts(classificationNodeId);
    // apply classification if existing
    if (facts != null && facts.classification != null) {
      ClassificationUtils.copyLinneanClassificationKeys(facts.classification, u);
      ClassificationUtils.copyLinneanClassification(facts.classification, u);
    }
    // convert to clb keys
    for (Rank r : Rank.DWC_RANKS) {
      ClassificationUtils.setHigherRankKey(u, r, clbKey(u.getHigherRankKey(r)));
    }
  }

  private List<Integer> subtreeBatch(Node startNode) {
    List<Integer> ids = Lists.newArrayList();
    try (Transaction tx = dao.beginTx()) {
      // returns all descendant nodes, accepted and synonyms but exclude pro parte relations!
      for (Node n : MultiRootNodeIterator.create(startNode, Traversals.TREE_WITHOUT_PRO_PARTE)) {
        ids.add((int)n.getId());
        if (isProParteNode(n)) {
          proParteNodes.add(n.getId());
        }
      }
    }
    LOG.debug("Created batch of {} nodes starting with {}", ids.size(), startNode);
    return ids;
  }

  private boolean isProParteNode(Node n) {
    if (n.hasRelationship(Direction.OUTGOING, RelType.PROPARTE_SYNONYM_OF)) {
      return true;
    }
    return false;
  }

  private void deleteOldUsages() {
    NameUsage first = nameUsageService.get(firstUsageKey, null);
    if (first == null || first.getLastInterpreted() == null) {
      LOG.error("First synced name usage with id {} not found", firstUsageKey);
      throw new EmptyImportException(datasetKey, "Error importing name usages for dataset " + datasetKey);
    }
    Calendar cal = Calendar.getInstance();
    cal.setTime(first.getLastInterpreted());
    // use 2 seconds before first insert/update as the threshold to remove records
    cal.add(Calendar.SECOND, -2);


    LOG.info("Deleting all usages before {}", cal.getTime());
    // iterate over all ids to be deleted and remove them from solr first
    List<Integer> ids = usageService.listOldUsages(datasetKey, cal.getTime());

    otherFutures.add(solrService.deleteUsages(datasetKey, ids));
    otherFutures.add(sqlService.deleteUsages(datasetKey, ids));
    delCounter = ids.size();
  }

  /**
   * Blocks until all currently listed futures are completed.
   */
  private void awaitOtherFutures() throws ExecutionException, InterruptedException {
    for (Future<?> f : otherFutures) {
      f.get();
    }
    otherFutures.clear();
  }

  private void awaitProParteFuture() throws ExecutionException, InterruptedException {
    if (proParteFuture != null) {
      // wait for pro parte pg sync.
      // solr doesnt need the parsed names
      otherFutures.add(solrService.sync(datasetKey, this, proParteFuture.get(), null));
    }
  }
  /**
   *
   * Waits for all core usages jobs to finish and submits solr updates for all of them once completed.
   */
  private void awaitUsageFutures() throws ExecutionException, InterruptedException {
    for (Future<List<Integer>> f : usageFutures) {
      List<Integer> ids = f.get();
      otherFutures.add(solrService.sync(datasetKey, this, ids));
    }
    usageFutures.clear();
  }

  private void clearFinishedUsageTasks() throws ExecutionException, InterruptedException {
    Iterator<Future<List<Integer>>> iter = usageFutures.iterator();
    while (iter.hasNext()) {
      Future<List<Integer>> f = iter.next();
      if (f.isDone()) {
        List<Integer> ids = f.get();
        otherFutures.add(solrService.sync(datasetKey, this, ids));
        iter.remove();
      }
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
      // missing key
      try (Transaction tx = dao.getNeo().beginTx()) {
        Node n = dao.getNeo().getNodeById(nodeId);
        LOG.error("Clb usage key missing for {}: {}", n, NeoProperties.getScientificName(n));
        NubUsage nub = dao.readNub(n);
        if (nub != null) {
          LOG.info("Nub usage for missing key: {}", nub.toStringComplete());
        } else {
          LOG.warn("Nub usage for missing key {} not found", nodeId);
        }
      } catch (Exception e) {
        // ignore, we throw anyways
      }
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
      if (!postKeys.containsKey(nid)) {
        postKeys.put(nid, new UsageForeignKeys(nid));
      }
      setFK(postKeys.get(nid), nodeFk, type);
      return null;
    }
  }

  private UsageForeignKeys setFK(UsageForeignKeys fk, Integer key, KeyType type) {
    if (key != null && type != null) {
      switch (type) {
        case BASIONYM:
          fk.setBasionymKey(key);
          break;
        case PARENT:
        case ACCEPTED:
          fk.setParentKey(key);
          break;
        case CLASSIFICATION:
          throw new IllegalArgumentException();
      }
    }
    return fk;
  }

  private NameUsage clone(NameUsage u) {
    Kryo kryo = kryoPool.borrow();
    try {
      // write
      ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);
      Output output = new Output(buffer, 256);
      kryo.writeObject(output, u);
      output.close();
      // read
      return kryo.readObject(new Input(buffer.toByteArray()), NameUsage.class);

    } finally {
      kryoPool.release(kryo);
    }
  }

  @Override
  public NameUsage readUsage(long id) {
    try (Transaction tx = dao.beginTx()) {
      Node n = dao.getNeo().getNodeById(id);
      return readUsage(n);
    }
  }

  @Override
  public ParsedName readName(long id) {
    if (Constants.NUB_DATASET_KEY.equals(datasetKey)) {
      return dao.readNubName(id);
    } else {
      return dao.readName(id);
    }
  }

  /**
   * Reads the full name usage from neo and updates all foreign keys to use CLB usage keys.
   */
  private NameUsage readUsage(Node n) {
    // this is using neo4j internal node ids as keys:
    NameUsage u = dao.readUsage(n, true);
    Preconditions.checkNotNull(u, "Node %s not found in kvp store", n.getId());
    Integer id = (int) n.getId();
    if (clbKeys.containsKey(id)) {
      u.setKey(clbKeys.get(id));
    }

    UsageFacts facts;
    if (n.hasLabel(Labels.SYNONYM)) {
      // use the classification of the parent in case of synonyms
      facts = dao.readFacts(u.getAcceptedKey());
      u.setTaxonomicStatus(u.getTaxonomicStatus() == null ? TaxonomicStatus.SYNONYM : u.getTaxonomicStatus());
      u.setAcceptedKey(clbForeignKey(n.getId(), u.getAcceptedKey(), KeyType.ACCEPTED));
    } else {
      facts = dao.readFacts(n.getId());
      u.setTaxonomicStatus(u.getTaxonomicStatus() == null ? TaxonomicStatus.ACCEPTED : u.getTaxonomicStatus());
      u.setParentKey(clbForeignKey(n.getId(), u.getParentKey(), KeyType.PARENT));
    }
    if (facts != null && facts.classification != null) {
      ClassificationUtils.copyLinneanClassificationKeys(facts.classification, u);
      ClassificationUtils.copyLinneanClassification(facts.classification, u);
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
    // update usage status and ppkey for primary pro parte usages
    if (isProParteNode(n)) {
      u.setTaxonomicStatus(TaxonomicStatus.PROPARTE_SYNONYM);
      u.setProParteKey(SELF_ID);
    }
    return u;
  }

  @Override
  public boolean isInsert(NameUsage u) {
    if (datasetKey.equals(Constants.NUB_DATASET_KEY)) {
      // for nub builds we generate the usageKey in code already. Both for inserts and updates. check key range
      return u.getKey() == null || u.getKey() > maxExistingNubKey;
    } else {
      return false;
    }
  }

  @Override
  public UsageExtensions readExtensions(long id) {
    return dao.readExtensions(id);
  }

  @Override
  public NameUsageMetrics readMetrics(long id) {
    UsageFacts facts = dao.readFacts(id);
    if (facts != null) {
      return facts.metrics;
    }
    return new NameUsageMetrics();
  }

  @Override
  public VerbatimNameUsage readVerbatim(long id) {
    return dao.readVerbatim(id);
  }

  @Override
  public List<Integer> readParentKeys(long id) {
    try (Transaction tx = dao.beginTx()) {
      Node n = dao.getNeo().getNodeById(id);
      return buildClbParents(n);
    }
  }

  @Override
  public void reportUsageKey(long nodeId, int usageKey) {
    if (datasetKey.equals(Constants.NUB_DATASET_KEY)) {
      Preconditions.checkArgument(usageKey < Constants.NUB_MAXIMUM_KEY, "New usage key {} for node {} is larger than allowed maximum {}", usageKey, nodeId, Constants.NUB_MAXIMUM_KEY);
    }
    // keep map of node ids to clb usage keys
    clbKeys.put( (int) nodeId, usageKey);
    // keep reference to first synced record in the db to get the modified timestamp from the db later.
    // this doesnt have to be exact so we do not need to worry about concurrent access much
    if (firstUsageKey < 0) {
      firstUsageKey = usageKey;
      LOG.info("First synced usage key is {}", firstUsageKey);
    }
  }

  @Override
  public void reportNewFuture(Future<List<Integer>> future) {
    otherFutures.add(future);
  }

  public int getSyncCounter() {
    return syncCounterMain + syncCounterBatches + syncCounterProParte;
  }

  public int getDelCounter() {
    return delCounter;
  }

}
