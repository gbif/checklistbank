package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.Constants;
import org.gbif.api.model.crawler.FinishReason;
import org.gbif.api.model.crawler.ProcessState;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.cli.common.RabbitDatasetService;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.checklistbank.cli.registry.RegistryService;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.index.guice.Solr;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.Mybatis;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImporterService extends RabbitDatasetService<ChecklistNormalizedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(ImporterService.class);

  private final ImporterConfiguration cfg;
  private DatasetImportService sqlService;
  private DatasetImportService solrService;
  private NameUsageService nameUsageService;
  private UsageService usageService;
  private final ZookeeperUtils zkUtils;

  public ImporterService(ImporterConfiguration cfg) {
    super("clb-importer", cfg.poolSize, cfg.messaging, cfg.ganglia, "import", ChecklistBankServiceMyBatisModule.create(cfg.clb), new RealTimeModule(cfg.solr));
    this.cfg = cfg;
    if (cfg.zookeeper.isConfigured()) {
      try {
        zkUtils = new ZookeeperUtils(cfg.zookeeper.getCuratorFramework());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      LOG.warn("Zookeeper not configured. Crawl metadata will not be managed.");
      zkUtils = null;
    }
    // init mybatis layer and solr once from cfg instance
    sqlService = getInstance(Key.get(DatasetImportService.class, Mybatis.class));
    solrService = getInstance(Key.get(DatasetImportService.class, Solr.class));
    nameUsageService = getInstance(NameUsageService.class);
    usageService = getInstance(UsageService.class);
  }

  @Override
  protected void process(ChecklistNormalizedMessage msg) throws Exception {
    try {
      Importer importer = Importer.create(cfg, msg.getDatasetUuid(), nameUsageService, usageService, sqlService, solrService);
      importer.run();

      // notify rabbit
      Date crawlFinished;
      if (cfg.zookeeper.isConfigured()) {
        crawlFinished = zkUtils.getDate(msg.getDatasetUuid(), ZookeeperUtils.FINISHED_CRAWLING);
        if (crawlFinished == null) {
          LOG.warn("No crawlFinished date found in zookeeper, use current date instead for dataset {}", msg.getDatasetUuid());
          crawlFinished = new Date();
        }
      } else {
        crawlFinished = new Date();
      }

      send(new ChecklistSyncedMessage(msg.getDatasetUuid(), crawlFinished, importer.getSyncCounter(), importer.getDelCounter()));
      // finally delete artifacts unless configured not to or it is the nub!
      if (cfg.deleteNeo && !Constants.NUB_DATASET_KEY.equals(msg.getDatasetUuid())) {
        RegistryService.deleteStorageFiles(cfg.neo, msg.getDatasetUuid());
      }

    } finally {
      if (cfg.zookeeper.isConfigured()) {
        zkUtils.createOrUpdate(msg.getDatasetUuid(), ZookeeperUtils.PROCESS_STATE_CHECKLIST, ProcessState.FINISHED);
      }
    }
  }

  @Override
  protected void failed(UUID datasetKey) {
    if (cfg.zookeeper.isConfigured()) {
      zkUtils.createOrUpdate(datasetKey, ZookeeperUtils.FINISHED_REASON, FinishReason.ABORT);
    }
  }

  @Override
  @VisibleForTesting
  protected void startUp() throws Exception {
    super.startUp();
  }

  @Override
  protected void shutDown() throws Exception {
    sqlService.close();
    solrService.close();
    super.shutDown();
  }

  @Override
  public Class<ChecklistNormalizedMessage> getMessageClass() {
    return ChecklistNormalizedMessage.class;
  }

}
