package org.gbif.checklistbank.nub;

import org.gbif.api.model.Constants;
import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.checklistbank.cli.normalizer.UsageMetricsAndNubMatchHandler;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.neo.traverse.TaxonWalker;

import java.util.LinkedList;
import java.util.List;

import com.beust.jcommander.internal.Lists;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NubBuilder extends NubDb implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(NubBuilder.class);
  private static final String P_KEY = "key";

  private final NubConfiguration cfg;
  private final NameUsageMatchingService matchingService;
  private final UsageSource usageSource;
  private NubSource currSrc;
  private LinkedList<Node> parents = Lists.newLinkedList();
  private int sourceUsageCounter = 0;

  private NubBuilder(NubConfiguration cfg, UsageSource usageSource) {
    super(cfg.neo.newEmbeddedDb(Constants.NUB_DATASET_KEY), 1000);
    this.cfg = cfg;
    this.usageSource = usageSource;
    matchingService = cfg.matching.createMatchingService();
  }

  public static NubBuilder create(NubConfiguration cfg) {
    return new NubBuilder(cfg, new ClbUsageSource(cfg));
  }

  public static NubBuilder create(NubConfiguration cfg, UsageSource usageSource) {
    return new NubBuilder(cfg, usageSource);
  }

  @Override
  public void run() {
    addDatasets();
    addImplicitTaxa();
    setEmptyGroupsDoubtful();
    groupByOriginalName();
    addExtensionData();
    assignUsageKeysAndMetrics();
    close();
  }


  /**
   * TODO: to be implemented.
   * Now clb still dynamically retrieves extension data from all checklists, but in the future we like to control
   * which extension record is attached to a backbone usage.
   *
   * Adds all extension data, e.g. vernacular names, to the backbone directly.
   * TODO:
   *  - build map from source usage key to nub node id
   *  - stream (jdbc copy) through all extension data in postgres and attach to relevant nub node
   */
  private void addExtensionData() {
    LOG.info("No extension data copied to backbone");
  }

  private void setEmptyGroupsDoubtful() {
    LOG.info("flag empty genera as doubtful");
  }

  private void addImplicitTaxa() {
    LOG.info("Start adding implicit taxa");
  }

  private void addDatasets() {
    List<NubSource> sources = usageSource.listSources();
    LOG.info("Start adding {} backbone sources", sources.size());
    for (NubSource source : sources) {
      addDataset(source);
    }
  }

  private void addDataset(NubSource source) {
    LOG.info("Adding source {}", source.name);
    currSrc = source;
    parents.clear();
    int start = sourceUsageCounter;
    for (SrcUsage u : usageSource.iterateSource(source)) {
      addUsage(u);
    }
    renewTx();
    LOG.info("Processed {} source usages for {}", sourceUsageCounter - start, source.name);
  }

  private void addUsage(SrcUsage u) {
    sourceUsageCounter++;
    Node parent = null;
    while (!parents.isEmpty()) {
      if (parents.getLast().getProperty(P_KEY).equals(u.parentKey)) {
        parent = parents.getLast();
        break;
      } else {
        parents.removeLast();
      }
    }
    if (u.parentKey != null && parent == null) {
      LOG.error("Parent node {} not found for {}", u.parentKey, u.canonical);
    }
    Node n = super.addUsage(parent, u);
    parents.add(n);
  }

  private void groupByOriginalName() {
    LOG.info("Start grouping by original names");
  }

  /**
   * Assigns a unique usageKey to all nodes by matching a usage to the previous backbone to keep stable identifiers.
   */
  private void assignUsageKeysAndMetrics() {
    LOG.info("Walk all accepted taxa, build metrics and match to previous GBIF backbone");
    UsageMetricsAndNubMatchHandler metricsHandler = new UsageMetricsAndNubMatchHandler(matchingService, db);
    TaxonWalker.walkAccepted(db, metricsHandler, 10000, null);
    renewTx();
    NormalizerStats stats = metricsHandler.getStats(0, null);
    LOG.info("Walked all taxa (root={}, total={}, synonyms={}) and built usage metrics", stats.getRoots(), stats.getCount(), stats.getSynonyms());
  }

}
