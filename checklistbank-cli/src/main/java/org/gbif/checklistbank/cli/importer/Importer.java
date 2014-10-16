package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.NeoRunnable;
import org.gbif.checklistbank.index.NameUsageIndexService;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.TaxonProperties;
import org.gbif.checklistbank.neo.traverse.TaxonomicNodeIterator;
import org.gbif.checklistbank.service.DatasetImportService;

import java.util.Calendar;
import java.util.UUID;
import javax.annotation.Nullable;

import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.yammer.metrics.Meter;
import com.yammer.metrics.MetricRegistry;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class Importer extends NeoRunnable implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(Importer.class);

  private final ImporterConfiguration cfg;
  private final Meter syncMeter;
  private int syncCounter;
  private int delCounter;
  private int failedCounter;
  private final Meter basionymMeter;
  private final DatasetImportServiceCombined importService;
  private final NameUsageService usageService;
  // neo internal ids to clb usage keys
  private IntIntOpenHashMap clbKeys = new IntIntOpenHashMap();
  // map based around internal neo4j node ids:
  private IntObjectOpenHashMap<Integer[]> postKeys = new IntObjectOpenHashMap<Integer[]>();
  private enum KeyType {PARENT, ACCEPTED, BASIONYM, PROPARTE};
  private final int keyTypeSize = KeyType.values().length;

  public Importer(ImporterConfiguration cfg, UUID datasetKey, MetricRegistry registry) {
    super(datasetKey, cfg.neo, registry);
    this.cfg = cfg;
    this.syncMeter = registry.getMeters().get(ImporterService.SYNC_METER);
    this.basionymMeter = registry.getMeters().get(ImporterService.SYNC_BASIONYM_METER);
    // init mybatis layer and solr from cfg instance
    Injector inj = Guice.createInjector(cfg.clb.createServiceModule(), new RealTimeModule(cfg.solr));
    importService = new DatasetImportServiceCombined(inj.getInstance(DatasetImportService.class),
                                                     inj.getInstance(NameUsageIndexService.class));
    usageService = inj.getInstance(NameUsageService.class);
  }

  public void run() {
    LOG.info("Start importing checklist {}", datasetKey);
    setupDb();
    syncDataset();
    tearDownDb();
    LOG.info("Importing of {} finished. Neo database shut down.", datasetKey);
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

    try (Transaction tx = db.beginTx()) {
      // returns all nodes, accepted and synonyms
      int counter = 0;
      for (Node n : TaxonomicNodeIterator.all(db)) {
        try {
          VerbatimNameUsage verbatim = mapper.readVerbatim(n);
          NameUsageContainer u = buildClbNameUsage(n);
          NameUsageMetrics metrics = mapper.read(n, new NameUsageMetrics());
          final int nodeId = (int) n.getId();
          final int usageKey = importService.syncUsage(u, verbatim, metrics);
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

        } catch (Exception e) {
          failedCounter++;
          Object taxID = n.getProperty(TaxonProperties.TAXON_ID, null);
          LOG.error("Failed to sync taxonID '{}' from dataset {}", taxID, datasetKey, e);
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
    NameUsage first = usageService.get(firstUsageKey, null);
    if (first == null) {
      LOG.error("No records imported or first name usage with id {} not found", firstUsageKey);
      throw new IllegalStateException("We did not seem to import a single name usage!");
    }

    Calendar cal = Calendar.getInstance();
    cal.setTime(first.getLastInterpreted());
    // use 2 seconds before first insert/update as the threshold to remove records
    cal.add(Calendar.SECOND, -2);
    delCounter = importService.deleteOldUsages(datasetKey, cal.getTime());
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
   * Maps a neo node id of a foreing key to an already created clb postgres id.
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
  protected NameUsageContainer buildClbNameUsage(Node n) {
    // this is using neo4j internal node ids as keys:
    NameUsageContainer u = mapper.read(n);
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

  public int getFailedCounter() {
    return failedCounter;
  }
}
