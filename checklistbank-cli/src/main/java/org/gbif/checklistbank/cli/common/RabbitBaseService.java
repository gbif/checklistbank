package org.gbif.checklistbank.cli.common;

import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.DatasetBasedMessage;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.io.IOException;
import java.util.UUID;
import javax.sql.DataSource;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.yammer.metrics.Counter;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;
import com.yammer.metrics.jvm.FileDescriptorRatioGauge;
import com.yammer.metrics.jvm.MemoryUsageGaugeSet;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RabbitBaseService<T extends Message> extends AbstractIdleService implements MessageCallback<T> {
  private static final Logger LOG = LoggerFactory.getLogger(RabbitBaseService.class);

  private final MessagingConfiguration mCfg;
  private final GangliaConfiguration gCfg;
  private final int poolSize;
  private final String queue;
  protected final MetricRegistry registry;
  private final Timer timer;
  private final Counter succeeded;
  private final Counter failed;
  protected HikariDataSource hds;
  protected MessagePublisher publisher;
  private MessageListener listener;

  public RabbitBaseService(String queue, int poolSize, MessagingConfiguration mCfg, GangliaConfiguration gCfg) {
    this.mCfg = mCfg;
    this.gCfg = gCfg;
    this.poolSize = poolSize;
    this.queue = queue;
    registry = new MetricRegistry(queue);
    registry.registerAll(new MemoryUsageGaugeSet());
    registry.register(Metrics.OPEN_FILES, new FileDescriptorRatioGauge());
    timer = registry.timer(regName("time"));
    succeeded = registry.counter(regName("succeeded"));
    failed = registry.counter(regName("failed"));
  }

  protected String regName(String name) {
    return registry.getName()+"."+name;
  }

  /**
   * Gets the clb DataSource instance from the existing guice injector.
   * Make sure the injector has the ChecklistBankServiceMyBatisModule bound.
   */
  protected void initDbPool(Injector inj) {
    Key<DataSource> dsKey = Key.get(DataSource.class,
      Names.named(InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME));
    hds = (HikariDataSource) inj.getInstance(dsKey);
  }

  @Override
  protected void startUp() throws Exception {
    gCfg.start(registry);

    publisher = new DefaultMessagePublisher(mCfg.getConnectionParameters());

    // dataset messages are slow, long running processes. Only prefetch one message
    listener = new MessageListener(mCfg.getConnectionParameters(), 1);
    listener.listen(queue, poolSize, this);
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
  public void handleMessage(T msg) {
    final Timer.Context context = timer.time();
    try {
      if (!ignore(msg)) {
        process(msg);
        succeeded.inc();
      }
    } catch (Throwable e) {
      final UUID datasetKey = msg instanceof DatasetBasedMessage ? ((DatasetBasedMessage) msg ).getDatasetUuid() : null;
      if (datasetKey != null) {
        LOG.error("Failed to process dataset {}", datasetKey, e);
        failed(datasetKey);
      } else {
        LOG.error("Failed to process {} message", msg.getClass().getSimpleName(), e);
      }
      failed.inc();
    } finally {
      context.stop();
    }
  }

  /**
   * Implement this filter method to ignore messages.
   * The default implementation accepts all incoming messages.
   * @param msg the message to check
   * @return true if the message should be ignored
   */
  protected boolean ignore(T msg) {
    return false;
  }

  /**
   * Implement this to do the real work.
   * The method is allowed to throw any exceptions which will be handled by this class.
   * Basic ganglia timer and succeeded/failed counter are also implemented already.
   */
  protected abstract void process(T msg) throws Exception;

  /**
   * Optional hook to implement when the message processing threw an exception.
   * @param datasetKey the dataset being processed
   */
  protected void failed(UUID datasetKey){
  }

  protected void send(DatasetBasedMessage msg) throws IOException {
    try {
      LOG.info("Sending {} for dataset {}", msg.getClass().getSimpleName(), msg.getDatasetUuid());
      publisher.send(msg);
    } catch (IOException e) {
      LOG.error("Could not send {} for dataset [{}]", msg.getClass().getSimpleName(), msg.getDatasetUuid(), e);
      throw e;
    }
  }

  protected void send(Message msg) throws IOException {
    try {
      LOG.info("Sending {}", msg.getClass().getSimpleName());
      publisher.send(msg);
    } catch (IOException e) {
      LOG.error("Could not send {}", msg.getClass().getSimpleName(), e);
      throw e;
    }
  }
}
