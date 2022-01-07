/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.Constants;
import org.gbif.api.model.crawler.FinishReason;
import org.gbif.api.model.crawler.ProcessState;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.cli.common.Metrics;
import org.gbif.checklistbank.cli.common.RabbitDatasetService;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;
import org.gbif.common.messaging.api.messages.DwcaMetasyncFinishedMessage;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalizerService extends RabbitDatasetService<DwcaMetasyncFinishedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(NormalizerService.class);

  private final NormalizerConfiguration cfg;
  private final ZookeeperUtils zkUtils;
  private IdLookup lookup;
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
  protected void initMetrics() {
    super.initMetrics();
    getRegistry().meter(Metrics.INSERT_METER);
    getRegistry().meter(Metrics.RELATION_METER);
    getRegistry().meter(Metrics.METRICS_METER);
    getRegistry().meter(Metrics.DENORMED_METER);
  }

  @Override
  protected void startUpBeforeListening() throws Exception {
    // loads all nub usages directly from clb postgres - this can take a few minutes
    lookup = IdLookupImpl.temp().load(cfg.clb, false);
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
        zkUtils.updateCounter(msg.getDatasetUuid(), ZookeeperUtils.PAGES_FRAGMENTED_SUCCESSFUL, 1L);
      }
      send(new ChecklistNormalizedMessage(msg.getDatasetUuid()));
    }
  }

  @Override
  protected void failed(UUID datasetKey) {
    if (cfg.zookeeper.isConfigured()) {
      zkUtils.createOrUpdate(datasetKey, ZookeeperUtils.FINISHED_REASON, FinishReason.ABORT);
      zkUtils.createOrUpdate(datasetKey, ZookeeperUtils.PROCESS_STATE_CHECKLIST, ProcessState.FINISHED);
      zkUtils.updateCounter(datasetKey, ZookeeperUtils.PAGES_FRAGMENTED_ERROR, 1L);
    }
  }

  @Override
  public Class<DwcaMetasyncFinishedMessage> getMessageClass() {
    return DwcaMetasyncFinishedMessage.class;
  }
}
