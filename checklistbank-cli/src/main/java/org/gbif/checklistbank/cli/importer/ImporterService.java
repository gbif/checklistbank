package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.crawler.FinishReason;
import org.gbif.api.model.crawler.ProcessState;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.cli.common.RabbitBaseService;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.checklistbank.index.NameUsageIndexService;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImporterService extends RabbitBaseService<ChecklistNormalizedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(ImporterService.class);

  public static final String SYNC_METER = "clb-importer.sync";

  private final ImporterConfiguration cfg;
  private DatasetImportServiceCombined importService;
  private NameUsageService usageService;
  private final ZookeeperUtils zkUtils;

  public ImporterService(ImporterConfiguration cfg) {
    super("clb-importer", cfg.poolSize, cfg.messaging, cfg.ganglia);
    this.cfg = cfg;
    registry.meter(SYNC_METER);
    try {
      zkUtils = new ZookeeperUtils(cfg.zookeeper.getCuratorFramework());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // init mybatis layer and solr from cfg instance
    Injector inj = Guice.createInjector(cfg.clb.createServiceModule(), new RealTimeModule(cfg.solr));
    initDbPool(inj);
    importService = new DatasetImportServiceCombined(inj.getInstance(DatasetImportService.class), inj.getInstance(NameUsageIndexService.class));
    usageService = inj.getInstance(NameUsageService.class);
  }

  @Override
  protected void process(ChecklistNormalizedMessage msg) throws Exception {
    try {
      Importer importer = new Importer(cfg, msg.getDatasetUuid(), registry, importService, usageService);
      importer.run();
      // notify rabbit
      Date crawlFinished = zkUtils.getDate(msg.getDatasetUuid(), ZookeeperUtils.FINISHED_CRAWLING);
      if (crawlFinished==null) {
        LOG.warn("No crawlFinished date found in zookeeper, use current date instead for dataset {}", msg.getDatasetUuid());
        crawlFinished=new Date();
      }
      send(new ChecklistSyncedMessage(msg.getDatasetUuid(), crawlFinished, importer.getSyncCounter(),
        importer.getDelCounter()));
      // cleanup neo files
      deleteDb(msg.getDatasetUuid());

    } finally {
      zkUtils.createOrUpdate(msg.getDatasetUuid(), ZookeeperUtils.PROCESS_STATE_CHECKLIST, ProcessState.FINISHED);
    }
  }

  @Override
  protected void failed(UUID datasetKey) {
    zkUtils.createOrUpdate(datasetKey, ZookeeperUtils.FINISHED_REASON, FinishReason.ABORT);
  }

  private void deleteDb(UUID datasetKey) {
    if (cfg.deleteNeo) {
      File db = cfg.neo.neoDir(datasetKey);
      try {
        FileUtils.deleteDirectory(db);
        LOG.info("Deleted neo database {}", db.getAbsoluteFile());
      } catch (IOException e) {
        LOG.error("Unable to delete neo database {}", db.getAbsoluteFile());
      }
    }
  }

  @Override
  @VisibleForTesting
  protected void startUp() throws Exception {
    super.startUp();
  }

  @Override
  public Class<ChecklistNormalizedMessage> getMessageClass() {
    return ChecklistNormalizedMessage.class;
  }
}
