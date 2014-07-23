package org.gbif.checklistbank.cli.importer;

import org.gbif.checklistbank.cli.postal.ChecklistNormalizedMessage;
import org.gbif.checklistbank.cli.postal.ChecklistSyncedMessage;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;

import java.io.IOException;

import com.google.common.util.concurrent.AbstractIdleService;
import com.yammer.metrics.Counter;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImporterService extends AbstractIdleService implements MessageCallback<ChecklistNormalizedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(ImporterService.class);

  public static final String SYNC_METER = "taxon.sync";
  public static final String SYNC_BASIONYM_METER = "taxon.sync.basionym";
  public static final String DELETE_TIMER = "taxon.sync.delete";

  private final ImporterConfiguration cfg;
  private MessageListener listener;
  private MessagePublisher publisher;
  private final MetricRegistry registry = new MetricRegistry("importer");
  private final Timer timer = registry.timer("normalizer process time");
  private final Counter started = registry.counter("started normalizations");
  private final Counter failed = registry.counter("failed normalizations");

  public ImporterService(ImporterConfiguration configuration) {
    this.cfg = configuration;
    registry.meter(SYNC_METER);
    registry.meter(SYNC_BASIONYM_METER);
    registry.timer(DELETE_TIMER);
  }

  @Override
  protected void startUp() throws Exception {
    cfg.ganglia.start(registry);

    publisher = new DefaultMessagePublisher(cfg.messaging.getConnectionParameters());

    listener = new MessageListener(cfg.messaging.getConnectionParameters());
    listener.listen(cfg.messaging.queueName, cfg.messaging.poolSize, this);
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
      Importer normalizer = new Importer(cfg, msg.getDatasetUuid(), registry);
      normalizer.run();
      started.inc();
      Message doneMsg = new ChecklistSyncedMessage(msg.getDatasetUuid());
      LOG.debug("Sending ChecklistSyncedMessage for dataset [{}]", msg.getDatasetUuid());
      publisher.send(doneMsg);

    } catch (IOException e) {
      LOG.warn("Could not send ChecklistSyncedMessage for dataset [{}]", msg.getDatasetUuid(), e);

    } finally {
      context.stop();
    }
  }

  @Override
  public Class<ChecklistNormalizedMessage> getMessageClass() {
    return ChecklistNormalizedMessage.class;
  }
}
