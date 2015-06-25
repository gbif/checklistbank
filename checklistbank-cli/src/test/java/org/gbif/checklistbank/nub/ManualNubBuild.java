package org.gbif.checklistbank.nub;

import org.gbif.api.model.Constants;
import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.checklistbank.cli.common.Metrics;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.UsageMetricsHandler;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.neo.traverse.TaxonWalker;

import java.io.File;

import com.yammer.metrics.Meter;
import com.yammer.metrics.MetricRegistry;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManualNubBuild {
  private static final Logger LOG = LoggerFactory.getLogger(ManualNubBuild.class);

  private static NubConfiguration local() {
    NubConfiguration cfg = new NubConfiguration();
    cfg.registry.wsUrl = "http://api.gbif-uat.org/v1";
    cfg.matching.matchWsUrl = null;
    cfg.neo.neoRepository = new File("/Users/markus/Desktop/repo");
    cfg.clb.serverName="localhost";
    cfg.clb.databaseName="clb";
    cfg.clb.user ="postgres";
    cfg.clb.password="pogo";
    return cfg;
  }

  private static NubConfiguration uat() {
    NubConfiguration cfg = new NubConfiguration();
    cfg.registry.wsUrl = "http://api.gbif-uat.org/v1";
    cfg.matching.matchWsUrl = null;
    cfg.neo.neoRepository = new File("/Users/markus/Desktop/repo");
    cfg.clb.serverName="pg1.gbif-uat.org";
    cfg.clb.databaseName="checklistbank";
    cfg.clb.user ="clb";
    cfg.clb.password="%BBJu2MgstXJ";
    return cfg;
  }

  private static void build(NubConfiguration cfg) {
    LOG.info("Build new nub");
    MetricRegistry registry = new MetricRegistry("nub-build");
    UsageDao dao = UsageDao.persistentDao(cfg.neo, Constants.NUB_DATASET_KEY, registry, true);
    NubBuilder builder = NubBuilder.create(dao, cfg.usageSource(), cfg.matching.createMatchingService());
    builder.run();
    dao.close();
    LOG.info("New backbone ready");
  }

  private static void profileMetrics(NubConfiguration cfg) {
    LOG.info("Open neo database");
    MetricRegistry registry = new MetricRegistry("nub-build");
    UsageDao dao = UsageDao.persistentDao(cfg.neo, Constants.NUB_DATASET_KEY, registry, false);
    UsageMetricsHandler metricsHandler = new UsageMetricsHandler(dao);
    LOG.info("Walk all accepted taxa and build usage metrics");
    TaxonWalker.walkAccepted(dao.getNeo(), registry.meter(Metrics.METRICS_METER), metricsHandler);
    NormalizerStats stats = metricsHandler.getStats(0, null);
    LOG.info("Walked all taxa (root={}, total={}, synonyms={}) and built usage metrics", stats.getRoots(), stats.getCount(), stats.getSynonyms());
  }

  public static void main(String[] args) {
    //build(uat());
    profileMetrics(local());
  }
}