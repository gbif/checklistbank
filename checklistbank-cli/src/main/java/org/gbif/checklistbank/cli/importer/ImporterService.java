package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.crawler.FinishReason;
import org.gbif.api.model.crawler.ProcessState;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;

import java.io.IOException;
import java.util.Date;

import com.google.common.util.concurrent.AbstractIdleService;
import com.yammer.metrics.Counter;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImporterService extends AbstractIdleService implements MessageCallback<ChecklistNormalizedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(ImporterService.class);

  public static final String QUEUE = "clb-importer";

  public static final String SYNC_METER = "taxon.sync";
  public static final String SYNC_BASIONYM_METER = "taxon.sync.basionym";
  public static final String DELETE_TIMER = "taxon.sync.delete";

  private final ImporterConfiguration cfg;
  private MessageListener listener;
  private MessagePublisher publisher;
  private final MetricRegistry registry = new MetricRegistry("importer");
  private final Timer timer = registry.timer("importer process time");
  private final Counter started = registry.counter("started imports");
  private final Counter failed = registry.counter("failed imports");
  private final ZookeeperUtils zkUtils;

  public ImporterService(ImporterConfiguration configuration) {
    this.cfg = configuration;
    registry.meter(SYNC_METER);
    registry.meter(SYNC_BASIONYM_METER);
    registry.timer(DELETE_TIMER);
    try {
      zkUtils = new ZookeeperUtils(configuration.zookeeper.getCuratorFramework());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void startUp() throws Exception {
    cfg.ganglia.start(registry);

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
  }

  @Override
  public void handleMessage(ChecklistNormalizedMessage msg) {
    final Timer.Context context = timer.time();

    try {
      Importer importer = new Importer(cfg, msg.getDatasetUuid(), registry);
      started.inc();
      importer.run();
      try {
        Date crawlFinished = zkUtils.getDate(msg.getDatasetUuid(), ZookeeperUtils.FINISHED_CRAWLING);
        Message doneMsg = new ChecklistSyncedMessage(msg.getDatasetUuid(), crawlFinished,
            importer.getSyncCounter(), importer.getDelCounter());
        LOG.info("Sending ChecklistSyncedMessage for dataset [{}], synced={}, deleted={}, ", msg.getDatasetUuid(), importer.getSyncCounter(), importer.getDelCounter());
        publisher.send(doneMsg);
      } catch (IOException e) {
        LOG.warn("Could not send ChecklistSyncedMessage for dataset [{}]", msg.getDatasetUuid(), e);
        zkUtils.createOrUpdate(msg.getDatasetUuid(), ZookeeperUtils.FINISHED_REASON, FinishReason.ABORT);
      }

    } catch (Throwable e) {
      failed.inc();
      LOG.error("Unknown error while importing dataset [{}]", msg.getDatasetUuid(), e);
      zkUtils.createOrUpdate(msg.getDatasetUuid(), ZookeeperUtils.FINISHED_REASON, FinishReason.ABORT);

    } finally {
      context.stop();
      zkUtils.createOrUpdate(msg.getDatasetUuid(), ZookeeperUtils.PROCESS_STATE_CHECKLIST, ProcessState.FINISHED);
    }
  }

  @Override
  public Class<ChecklistNormalizedMessage> getMessageClass() {
    return ChecklistNormalizedMessage.class;
  }
}
