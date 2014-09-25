package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.crawler.FinishReason;
import org.gbif.api.model.crawler.ProcessState;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;
import org.gbif.common.messaging.api.messages.DwcaMetasyncFinishedMessage;

import java.io.IOException;
import java.util.UUID;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.yammer.metrics.Counter;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;
import com.yammer.metrics.jvm.MemoryUsageGaugeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalizerService extends AbstractIdleService implements MessageCallback<DwcaMetasyncFinishedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(NormalizerService.class);

  public static final String QUEUE = "clb-normalizer";

  public static final String INSERT_METER = "taxon.inserts";
  public static final String RELATION_METER = "taxon.relations";
  public static final String METRICS_METER = "taxon.metrics";
  public static final String DENORMED_METER = "taxon.denormed";
  public static final String HEAP_GAUGE = "heap.usage";

  private final NormalizerConfiguration cfg;
  private MessageListener listener;
  private MessagePublisher publisher;
  private final MetricRegistry registry = new MetricRegistry("normalizer");
  private final Timer timer = registry.timer("normalizer process time");
  private final Counter started = registry.counter("started normalizations");
  private final Counter failed = registry.counter("failed normalizations");
  private final ZookeeperUtils zkUtils;
  private NameUsageMatchingService matchingService;

  public NormalizerService(NormalizerConfiguration configuration) {
    this.cfg = configuration;

    MemoryUsageGaugeSet mgs = new MemoryUsageGaugeSet();
    registry.registerAll(mgs);
    registry.meter(INSERT_METER);
    registry.meter(RELATION_METER);
    registry.meter(METRICS_METER);
    registry.meter(DENORMED_METER);

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

    // use ws clients for nub matching
    Injector injClient = Guice.createInjector(cfg.createMatchClientModule());
    matchingService = injClient.getInstance(NameUsageMatchingService.class);
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
  public void handleMessage(DwcaMetasyncFinishedMessage msg) {
    final Timer.Context context = timer.time();

    try {

      if (msg.getDatasetType() != DatasetType.CHECKLIST) {
        LOG.info("Rejected dataset {} of type {}", msg.getDatasetUuid(), msg.getDatasetType());
        return;
      }
      Normalizer normalizer = new Normalizer(cfg, msg.getDatasetUuid(), registry, msg.getConstituents(), matchingService);
      started.inc();
      normalizer.run();
      Message doneMsg = new ChecklistNormalizedMessage(msg.getDatasetUuid(), normalizer.getStats());
      LOG.debug("Sending ChecklistNormalizedMessage for dataset [{}]", msg.getDatasetUuid());
      publisher.send(doneMsg);
      zkUtils.updateCounter(msg.getDatasetUuid(), ZookeeperUtils.PAGES_FRAGMENTED_SUCCESSFUL, 1l);

    } catch (IOException e) {
      error(msg.getDatasetUuid(), "Could not send ChecklistNormalizedMessage for dataset [{}]", e);

    } catch (NormalizationFailedException e) {
      failed.inc();
      error(msg.getDatasetUuid(), "Failed to normalize dataset {}", e);

    } catch (RuntimeException e) {
      error(msg.getDatasetUuid(), "Unknown error while importing dataset [{}]", e);

    } finally {
      context.stop();
    }
  }

  private void error(UUID datasetKey, String message, Throwable e){
    LOG.error(message, datasetKey, e);
    zkUtils.createOrUpdate(datasetKey, ZookeeperUtils.FINISHED_REASON, FinishReason.ABORT);
    zkUtils.createOrUpdate(datasetKey, ZookeeperUtils.PROCESS_STATE_CHECKLIST, ProcessState.FINISHED);
    zkUtils.updateCounter(datasetKey, ZookeeperUtils.PAGES_FRAGMENTED_ERROR, 1l);
  }

  @Override
  public Class<DwcaMetasyncFinishedMessage> getMessageClass() {
    return DwcaMetasyncFinishedMessage.class;
  }
}
