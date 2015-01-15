package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.crawler.FinishReason;
import org.gbif.api.model.crawler.ProcessState;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.checklistbank.index.NameUsageIndexService;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import javax.sql.DataSource;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.yammer.metrics.Counter;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImporterService extends AbstractIdleService implements MessageCallback<ChecklistNormalizedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(ImporterService.class);

  public static final String QUEUE = "clb-importer";


  private final ImporterConfiguration cfg;
  private MessageListener listener;
  private MessagePublisher publisher;
  private HikariDataSource hds;
  private DatasetImportServiceCombined importService;
  private NameUsageService usageService;
  private final MetricRegistry registry = new MetricRegistry("importer");
  public static final String SYNC_METER = "taxon.sync";
  private final Timer timer = registry.timer("importer.time");
  private final Counter started = registry.counter("importer.started");
  private final Counter failed = registry.counter("importer.failed");
  private final ZookeeperUtils zkUtils;

  public ImporterService(ImporterConfiguration configuration) {
    this.cfg = configuration;
    registry.meter(SYNC_METER);
    try {
      zkUtils = new ZookeeperUtils(configuration.zookeeper.getCuratorFramework());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void startUp() throws Exception {
    cfg.ganglia.start(registry);

    // init mybatis layer and solr from cfg instance
    Injector inj = Guice.createInjector(cfg.clb.createServiceModule(), new RealTimeModule(cfg.solr));
    importService = new DatasetImportServiceCombined(inj.getInstance(DatasetImportService.class), inj.getInstance(NameUsageIndexService.class));
    usageService = inj.getInstance(NameUsageService.class);
    Key<DataSource> dsKey = Key.get(DataSource.class, Names.named(InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME));
    hds = (HikariDataSource) inj.getInstance(dsKey);

    publisher = new DefaultMessagePublisher(cfg.messaging.getConnectionParameters());

    listener = new MessageListener(cfg.messaging.getConnectionParameters());
    listener.listen(QUEUE, cfg.messaging.poolSize, this);
  }

  @Override
  protected void shutDown() throws Exception {
    if (listener != null) {
      listener.close();
    }
    if (publisher != null) {
      publisher.close();
    }
    if (hds != null) {
      hds.close();
    }
  }

  @Override
  public void handleMessage(ChecklistNormalizedMessage msg) {
    final Timer.Context context = timer.time();

    try {
      Importer importer = new Importer(cfg, msg.getDatasetUuid(), registry, importService, usageService);
      started.inc();
      importer.run();
      try {
        Date crawlFinished = zkUtils.getDate(msg.getDatasetUuid(), ZookeeperUtils.FINISHED_CRAWLING);
        if (crawlFinished==null) {
          LOG.warn("No crawlFinished date found in zookeeper, use current date instead for dataset {}", msg.getDatasetUuid());
          crawlFinished=new Date();
        }
        Message doneMsg = new ChecklistSyncedMessage(msg.getDatasetUuid(), crawlFinished,
            importer.getSyncCounter(), importer.getDelCounter());
        LOG.info("Sending ChecklistSyncedMessage for dataset [{}], synced={}, deleted={}, ", msg.getDatasetUuid(), importer.getSyncCounter(), importer.getDelCounter());
        publisher.send(doneMsg);
      } catch (IOException e) {
        LOG.warn("Could not send ChecklistSyncedMessage for dataset [{}]", msg.getDatasetUuid(), e);
        zkUtils.createOrUpdate(msg.getDatasetUuid(), ZookeeperUtils.FINISHED_REASON, FinishReason.ABORT);
      }
      deleteDb(msg.getDatasetUuid());

    } catch (Throwable e) {
      failed.inc();
      LOG.error("Unknown error while importing dataset [{}]", msg.getDatasetUuid(), e);
      zkUtils.createOrUpdate(msg.getDatasetUuid(), ZookeeperUtils.FINISHED_REASON, FinishReason.ABORT);

    } finally {
      context.stop();
      zkUtils.createOrUpdate(msg.getDatasetUuid(), ZookeeperUtils.PROCESS_STATE_CHECKLIST, ProcessState.FINISHED);
    }
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
  public Class<ChecklistNormalizedMessage> getMessageClass() {
    return ChecklistNormalizedMessage.class;
  }
}
