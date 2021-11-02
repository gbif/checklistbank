package org.gbif.checklistbank.cli.common;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Sets;
import com.google.inject.Module;
import org.gbif.checklistbank.config.GangliaConfiguration;
import org.gbif.checklistbank.logging.LogContext;
import org.gbif.common.messaging.api.messages.DatasetBasedMessage;
import org.gbif.common.messaging.config.MessagingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public abstract class RabbitDatasetService<T extends DatasetBasedMessage> extends RabbitBaseService<T> {
  private static final Logger LOG = LoggerFactory.getLogger(RabbitDatasetService.class);
  private static final Marker DOI_SMTP = MarkerFactory.getMarker("SMTP");
  private Timer timer;
  private Counter succeeded;
  private Counter failed;
  protected Set<UUID> runningJobs = Sets.newHashSet();
  private final String action;

  public RabbitDatasetService(String queue, int poolSize, MessagingConfiguration mCfg, GangliaConfiguration gCfg, String action, Module... modules) {
    super(queue, poolSize, mCfg, gCfg, modules);
    this.action = action;
  }

  @Override
  protected void initMetrics(MetricRegistry registry) {
    super.initMetrics(registry);
    timer = registry.timer(regName("time"));
    succeeded = registry.counter(regName("succeeded"));
    failed = registry.counter(regName("failed"));
  }

  @Override
  public void handleMessage(T msg) {
    final Timer.Context context = timer.time();
    try {
      LogContext.startDataset(msg.getDatasetUuid());
      if (!ignore(msg)) {
        if (runningJobs.contains(msg.getDatasetUuid())) {
          LOG.warn("Dataset {} {} job already running. Ignore message", action, msg.getDatasetUuid());
        } else {
          runningJobs.add(msg.getDatasetUuid());
          process(msg);
          succeeded.inc();
          runningJobs.remove(msg.getDatasetUuid());
        }
      }
    } catch (Throwable e) {
      runningJobs.remove(msg.getDatasetUuid());
      LOG.error(DOI_SMTP, "Failed to {} dataset {}", action, msg.getDatasetUuid(), e);
      failed(msg.getDatasetUuid());
      failed.inc();
    } finally {
      context.stop();
      LogContext.endDataset();
    }
  }

  /**
   * Implement this filter method to ignore messages.
   * The default implementation accepts all incoming messages.
   *
   * @param msg the message to check
   *
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
   *
   * @param datasetKey the dataset being processed
   */
  protected void failed(UUID datasetKey) {
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

}
