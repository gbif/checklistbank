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
import org.gbif.checklistbank.neo.traverse.TaxonomicNodeIterator;
import org.gbif.checklistbank.service.DatasetImportService;

import java.util.Calendar;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.beust.jcommander.internal.Maps;
import com.carrotsearch.hppc.IntIntOpenHashMap;
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
  private final Meter basionymMeter;
  private final AtomicInteger failed = new AtomicInteger();
  private final DatasetImportServiceCombined importService;
  private final NameUsageService usageService;
  // neo internal ids to clb usage keys
  private IntIntOpenHashMap clbKeys = new IntIntOpenHashMap();
  // map based around internal neo4j node ids:
  private Map<Integer, Integer> basionyms = Maps.newHashMap();

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
    failed.set(0);

    // we keep the very first usage key to retrieve the exact last modified timestamp from the database
    // in order to avoid clock differences between machines and threads.
    int firstUsageKey = -1;

    try (Transaction tx = db.beginTx()) {
      for (Node n : TaxonomicNodeIterator.all(db)) {
        // returns all accepted and synonyms
        NameUsageContainer u = buildClbNameUsage(n);
        VerbatimNameUsage verbatim = mapper.readVerbatim(n);
        NameUsageMetrics metrics = mapper.read(n, new NameUsageMetrics());
        final int nodeId = (int) n.getId();
        // remember basionymKey if we have not processed it before already
        if (u.getBasionymKey() != null && !clbKeys.containsKey(u.getBasionymKey())) {
          // these are internal neo4j node ids!
          basionyms.put(nodeId, u.getBasionymKey());
          u.setBasionymKey(null);
        }
        try {
          final int usageKey = importService.syncUsage(datasetKey, u, verbatim, metrics);
          // keep map of node ids to clb usage keys
          clbKeys.put(nodeId, usageKey);
          if (firstUsageKey < 0) {
            firstUsageKey = usageKey;
            LOG.info("First synced usage key for dataset {} is {}", datasetKey, firstUsageKey);
          }
          syncMeter.mark();
          syncCounter++;
        } catch (Exception e) {
          failed.getAndIncrement();
          LOG.error("Failed to sync usage {} from dataset {}", u.getTaxonID(), datasetKey, e);
        }
      }
    }

    // fix basionyms
    if (!basionyms.isEmpty()) {
      LOG.info("Updating basionym keys for {} usages from dataset {}", basionyms.size(), datasetKey);
      importService.updateBasionyms(datasetKey, basionyms);
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
   * If the requested nodeID actually refers to the current node id, then -1 will be returned to indicate to the mybatis
   * mapper that it should use the newly generated sequence value.
   */
  private Integer clbKey(Node n, Integer nodeId) {
    if (nodeId != null) {
      if (clbKeys.containsKey(nodeId)) {
        return clbKeys.get(nodeId);
      } else if(n.getId() == (long) nodeId) {
        return -1;
      }
      throw new IllegalStateException("NodeId not in CLB yet: " + nodeId);
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
    u.setParentKey(clbKey(n, u.getParentKey()));
    u.setAcceptedKey(clbKey(n, u.getAcceptedKey()));
    u.setBasionymKey(clbKey(n, u.getBasionymKey()));
    u.setProParteKey(clbKey(n, u.getProParteKey()));
    for (Rank r : Rank.DWC_RANKS) {
      ClassificationUtils.setHigherRankKey(u, r, clbKey(n, u.getHigherRankKey(r)));
    }
    return u;
  }

  public int getSyncCounter() {
    return syncCounter;
  }

  public int getDelCounter() {
    return delCounter;
  }
}
