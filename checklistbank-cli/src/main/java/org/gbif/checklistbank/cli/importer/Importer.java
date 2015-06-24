package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.Metrics;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.neo.model.ClassificationKeys;
import org.gbif.checklistbank.neo.model.UsageFacts;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.ImportDb;
import org.gbif.checklistbank.neo.NodeProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.TaxonomicNodeIterator;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.yammer.metrics.Meter;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.jvm.FileDescriptorRatioGauge;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class Importer extends ImportDb implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(Importer.class);

  private final Meter syncMeter;
  private int syncCounter;
  private int delCounter;
  private final DatasetImportServiceCombined importService;
  private final NameUsageService usageService;
  // neo internal ids to clb usage keys
  private IntIntMap clbKeys = new IntIntHashMap();
  // map based around internal neo4j node ids:
  private IntObjectMap<Integer[]> postKeys = new IntObjectHashMap<Integer[]>();
  private enum KeyType {PARENT, ACCEPTED, BASIONYM, PROPARTE};
  private final int keyTypeSize = KeyType.values().length;

  public Importer(UUID datasetKey, UsageDao dao, MetricRegistry registry,
    DatasetImportServiceCombined importService, NameUsageService usageService) {
    super(datasetKey, dao);
    this.importService = importService;
    this.usageService = usageService;
    this.syncMeter = registry.meter(Metrics.SYNC_METER);
    registry.register(Metrics.SYNC_FILES, new FileDescriptorRatioGauge());
  }

  public static Importer create(NeoConfiguration cfg, UUID datasetKey, MetricRegistry registry,
                                DatasetImportServiceCombined importService, NameUsageService usageService) {
    return new Importer(datasetKey,
            UsageDao.persistentDao(cfg, datasetKey, registry, false),
            registry, importService, usageService);
  }

  public void run() {
    LOG.info("Start importing checklist {}", datasetKey);
    try {
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
   */
  private void syncDataset() {
    // we keep the very first usage key to retrieve the exact last modified timestamp from the database
    // in order to avoid clock differences between machines and threads.
    int firstUsageKey = -1;

    try (Transaction tx = dao.getNeo().beginTx()) {
      // returns all nodes, accepted and synonyms
      int counter = 0;
      for (Node n : TaxonomicNodeIterator.all(dao.getNeo())) {
        try {
          final int nodeId = (int) n.getId();
          VerbatimNameUsage verbatim = dao.readVerbatim(nodeId);
          UsageFacts facts = dao.readFacts(nodeId);
          if (facts == null){
            facts = new UsageFacts();
            facts.metrics = new NameUsageMetrics();
          }
          NameUsage u = buildClbNameUsage(n, facts.classification);
          List<Integer> parents = buildClbParents(n);
          UsageExtensions ext = dao.readExtensions(nodeId);

          final int usageKey = importService.syncUsage(u, parents, verbatim, facts.metrics, ext);
          // keep map of node ids to clb usage keys
          clbKeys.put(nodeId, usageKey);
          if (firstUsageKey < 0) {
            firstUsageKey = usageKey;
            LOG.info("First synced usage key for dataset {} is {}", datasetKey, firstUsageKey);
          }
          counter++;
          syncMeter.mark();
          syncCounter++;
          if (counter % 100000 == 0) {
            LOG.info("Synced {} usages from dataset {}, latest usage key={}", counter, datasetKey, usageKey);
          } else if (counter % 100 == 0) {
            LOG.debug("Synced {} usages from dataset {}, latest usage key={}", counter, datasetKey, usageKey);
          }

        } catch (Throwable e) {
          String id;
          if (n.hasProperty(NodeProperties.TAXON_ID)) {
            id = String.format("taxonID '%s'", n.getProperty(NodeProperties.TAXON_ID));
          } else {
            id = String.format("nodeID %s", n.getId());
          }
          LOG.error("Failed to sync {} {} from dataset {}", n.getProperty(NodeProperties.SCIENTIFIC_NAME, ""), id, datasetKey);
          LOG.error("Aborting sync of dataset {}", datasetKey);
          throw e;
        }
      }
    }

    // finally update foreign keys that did not exist during initial inserts
    if (!postKeys.isEmpty()) {
      LOG.info("Updating foreign keys for {} usages from dataset {}", postKeys.size(), datasetKey);
      for (IntObjectCursor<Integer[]> c : postKeys) {
        // update usage by usage doing all 3 potential updates in one statement
        Integer parentKey = c.value[KeyType.ACCEPTED.ordinal()];
        importService.updateForeignKeys(clbKey(c.key),
              clbKey(parentKey != null ? parentKey : c.value[KeyType.PARENT.ordinal()]),
              clbKey(c.value[KeyType.PROPARTE.ordinal()]),
              clbKey(c.value[KeyType.BASIONYM.ordinal()])
        );
      }
    }

    // remove old usages
    if (firstUsageKey < 0) {
      LOG.error("No records imported for dataset {}. Keep all existing data!", datasetKey);
      throw new IllegalStateException("No records imported for dataset " + datasetKey);

    } else {
      NameUsage first = usageService.get(firstUsageKey, null);
      if (first == null) {
        LOG.error("First name usage with id {} not found", firstUsageKey);
        throw new IllegalStateException("Error importing name usages for dataset " + datasetKey);
      }
      Calendar cal = Calendar.getInstance();
      cal.setTime(first.getLastInterpreted());
      // use 2 seconds before first insert/update as the threshold to remove records
      cal.add(Calendar.SECOND, -2);
      delCounter = importService.deleteOldUsages(datasetKey, cal.getTime());
    }
  }

  /**
   * @return list of parental node ids
   */
  private List<Integer> buildClbParents(Node n) {
    return com.google.common.collect.Lists
      .transform(IteratorUtil.asList(n.getRelationships(RelType.PARENT_OF, Direction.INCOMING)),
        new Function<Relationship, Integer>() {
          @Override
          public Integer apply(Relationship rel) {
            return rel != null ? clbKey((int)rel.getStartNode().getId()) : null;
          }
        });
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
   * @param nodeId the node id casted from long that represents the currently processed name usage record
   * @param nodeFk the foreign key to the node id we wanna setup the relation to
   */
  private Integer clbForeignKey(int nodeId, Integer nodeFk, @Nullable KeyType type) {
    if (nodeFk != null) {
      if (clbKeys.containsKey(nodeFk)) {
        return clbKeys.get(nodeFk);
      } else if(nodeId == nodeFk) {
        return -1;
      } else if(type != null) {
        // remember non classification keys for update after all records have been synced once
        if (postKeys.containsKey(nodeId)) {
          postKeys.get(nodeId)[type.ordinal()] = nodeFk;
        } else {
          Integer[] keys = new Integer[keyTypeSize];
          keys[type.ordinal()] = nodeFk;
          postKeys.put(nodeId, keys);
        }
      } else {
        throw new IllegalStateException("NodeId not in CLB yet: " + nodeFk);
      }
    }
    return null;
  }

  /**
   * Reads the full name usage from neo and updates all foreign keys to use CLB usage keys.
   */
  @VisibleForTesting
  protected NameUsage buildClbNameUsage(Node n, @Nullable ClassificationKeys classification) {
    // this is using neo4j internal node ids as keys:
    NameUsage u = dao.readUsage(n, true);
    if (classification != null) {
      ClassificationUtils.copyLinneanClassificationKeys(classification, u);
    }
    if (n.hasLabel(Labels.SYNONYM)) {
      u.setSynonym(true);
      u.setAcceptedKey(clbForeignKey((int) n.getId(), u.getAcceptedKey(), KeyType.ACCEPTED));
      u.setProParteKey(clbForeignKey((int) n.getId(), u.getProParteKey(), KeyType.PROPARTE));
    } else {
      u.setSynonym(false);
      u.setParentKey(clbForeignKey((int) n.getId(), u.getParentKey(), KeyType.PARENT));
    }
    u.setBasionymKey(clbForeignKey((int) n.getId(), u.getBasionymKey(), KeyType.BASIONYM));
    for (Rank r : Rank.DWC_RANKS) {
      ClassificationUtils.setHigherRankKey(u, r, clbForeignKey((int) n.getId(), u.getHigherRankKey(r), null));
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
