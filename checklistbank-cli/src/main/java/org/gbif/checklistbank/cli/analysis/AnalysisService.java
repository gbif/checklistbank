package org.gbif.checklistbank.cli.analysis;

import org.gbif.checklistbank.service.DatasetAnalysisService;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChecklistAnalyzedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;

import java.io.IOException;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalysisService extends AbstractIdleService implements MessageCallback<ChecklistSyncedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(AnalysisService.class);

  public static final String QUEUE = "clb-analysis";

  public static final String MATCH_METER = "checklist.analysis";

  private final AnalysisConfiguration cfg;
  private MessageListener listener;
  private MessagePublisher publisher;
  private final MetricRegistry registry = new MetricRegistry("checklist.analysis");
  private final Timer timer = registry.timer("checklist.analysis.time");
  private final DatasetAnalysisService analysisService;

  public AnalysisService(AnalysisConfiguration configuration) {
    this.cfg = configuration;
    registry.meter(MATCH_METER);
    Injector inj = Guice.createInjector(cfg.clb.createServiceModule());
    analysisService = inj.getInstance(DatasetAnalysisService.class);
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
  public void handleMessage(ChecklistSyncedMessage msg) {
    final Timer.Context context = timer.time();

    try {
      analysisService.analyse(msg.getDatasetUuid(), msg.getCrawlFinished());
      Message doneMsg = new ChecklistAnalyzedMessage(msg.getDatasetUuid());
      LOG.debug("Sending ChecklistAnalyzedMessage for dataset {}", msg.getDatasetUuid());
      publisher.send(doneMsg);

    } catch (IOException e) {
      LOG.warn("Could not send ChecklistMatchedMessage for dataset [{}]", msg.getDatasetUuid(), e);

    } finally {
      context.stop();
    }
  }

  @Override
  public Class<ChecklistSyncedMessage> getMessageClass() {
    return ChecklistSyncedMessage.class;
  }

}
