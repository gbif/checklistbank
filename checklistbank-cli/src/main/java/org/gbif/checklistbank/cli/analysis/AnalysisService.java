package org.gbif.checklistbank.cli.analysis;

import org.gbif.checklistbank.service.DatasetAnalysisService;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChecklistAnalyzedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;

import java.io.IOException;

import javax.sql.DataSource;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalysisService extends AbstractIdleService implements MessageCallback<ChecklistSyncedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(AnalysisService.class);

  private static final String QUEUE = "clb-analysis";
  private static final String MATCH_METER = "checklist.analysis";

  private final AnalysisConfiguration cfg;
  private MessageListener listener;
  private MessagePublisher publisher;
  private final MetricRegistry registry = new MetricRegistry("checklist.analysis");
  private final Timer timer = registry.timer("checklist.analysis.time");
  private final DatasetAnalysisService analysisService;
  private final HikariDataSource hds;

  public AnalysisService(AnalysisConfiguration configuration) {
    this.cfg = configuration;
    registry.meter(MATCH_METER);
    Injector inj = Guice.createInjector(cfg.clb.createServiceModule());
    analysisService = inj.getInstance(DatasetAnalysisService.class);
    Key<DataSource> dsKey = Key.get(DataSource.class, Names.named(InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME));
    hds = (HikariDataSource) inj.getInstance(dsKey);
  }

  @Override
  protected void startUp() throws Exception {
    cfg.ganglia.start(registry);

    publisher = new DefaultMessagePublisher(cfg.messaging.getConnectionParameters());

    listener = new MessageListener(cfg.messaging.getConnectionParameters(), 1);
    listener.listen(QUEUE, cfg.messaging.poolSize, this);
  }

  @Override
  protected void shutDown() throws Exception {
    if (hds != null) {
      hds.close();
    }
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
      try {
        Message doneMsg = new ChecklistAnalyzedMessage(msg.getDatasetUuid());
        LOG.info("Sending ChecklistAnalyzedMessage for dataset {}", msg.getDatasetUuid());
        publisher.send(doneMsg);
      } catch (IOException e) {
        LOG.warn("Could not send ChecklistMatchedMessage for dataset [{}]", msg.getDatasetUuid(), e);
      }

    } catch (Throwable e) {
      LOG.error("Failed to analyze dataset [{}]", msg.getDatasetUuid(), e);

    } finally {
      context.stop();
    }
  }

  @Override
  public Class<ChecklistSyncedMessage> getMessageClass() {
    return ChecklistSyncedMessage.class;
  }

}
