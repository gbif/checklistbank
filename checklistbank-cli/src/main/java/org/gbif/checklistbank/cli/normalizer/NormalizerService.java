package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.Constants;
import org.gbif.api.model.crawler.FinishReason;
import org.gbif.api.model.crawler.ProcessState;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.cli.common.Metrics;
import org.gbif.checklistbank.cli.common.RabbitDatasetService;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.checklistbank.service.MatchingService;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;
import org.gbif.common.messaging.api.messages.DwcaMetasyncFinishedMessage;
import org.gbif.nub.lookup.fuzzy.NubMatchingServiceImpl;

import java.io.IOException;
import java.util.UUID;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalizerService extends RabbitDatasetService<DwcaMetasyncFinishedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(NormalizerService.class);

  private final NormalizerConfiguration cfg;
  private final ZookeeperUtils zkUtils;
  private MatchingService lookup;
  private static final String QUEUE = "clb-normalizer";

  public NormalizerService(NormalizerConfiguration cfg) {
    super(QUEUE, cfg.poolSize, cfg.messaging, cfg.ganglia, "normalize");
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
  }

  @Override
  protected void initMetrics(MetricRegistry registry) {
    super.initMetrics(registry);
    registry.meter(Metrics.INSERT_METER);
    registry.meter(Metrics.RELATION_METER);
    registry.meter(Metrics.METRICS_METER);
    registry.meter(Metrics.DENORMED_METER);
  }

  @Override
  protected void startUpBeforeListening() throws Exception {
    // loads all nub usages directly from clb postgres - this can take a few minutes
    lookup = NubMatchingServiceImpl.strictMatchingIndex(cfg.clb);
  }

  @Override
  protected boolean ignore(DwcaMetasyncFinishedMessage msg) {
    if (msg.getDatasetType() != DatasetType.CHECKLIST) {
      LOG.info("Rejected dataset {} of type {}", msg.getDatasetUuid(), msg.getDatasetType());
      return true;
    }
    return false;
  }

  @Override
  protected void process(DwcaMetasyncFinishedMessage msg) throws Exception {
    if (Constants.NUB_DATASET_KEY.equals(msg.getDatasetUuid())) {
      LOG.warn("Refuse to normalize the GBIF backbone");
      failed(msg.getDatasetUuid());
    } else {
      Normalizer normalizer = Normalizer.create(cfg, msg.getDatasetUuid(), getRegistry(), msg.getConstituents(), lookup);
      normalizer.run();
      if (cfg.zookeeper.isConfigured()) {
        zkUtils.updateCounter(msg.getDatasetUuid(), ZookeeperUtils.PAGES_FRAGMENTED_SUCCESSFUL, 1l);
      }
      send(new ChecklistNormalizedMessage(msg.getDatasetUuid()));
    }
  }

  @Override
  protected void failed(UUID datasetKey) {
    if (cfg.zookeeper.isConfigured()) {
      zkUtils.createOrUpdate(datasetKey, ZookeeperUtils.FINISHED_REASON, FinishReason.ABORT);
      zkUtils.createOrUpdate(datasetKey, ZookeeperUtils.PROCESS_STATE_CHECKLIST, ProcessState.FINISHED);
      zkUtils.updateCounter(datasetKey, ZookeeperUtils.PAGES_FRAGMENTED_ERROR, 1l);
    }
  }

  @Override
  public Class<DwcaMetasyncFinishedMessage> getMessageClass() {
    return DwcaMetasyncFinishedMessage.class;
  }
}
