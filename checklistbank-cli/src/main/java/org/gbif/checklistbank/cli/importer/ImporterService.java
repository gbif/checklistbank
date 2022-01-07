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
package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.Constants;
import org.gbif.api.model.crawler.FinishReason;
import org.gbif.api.model.crawler.ProcessState;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.cli.common.RabbitDatasetService;
import org.gbif.checklistbank.cli.common.SpringContextBuilder;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.checklistbank.cli.registry.RegistryService;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.service.DatasetImportServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.NameUsageServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.ParsedNameServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.UsageServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.UsageSyncServiceMyBatis;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class ImporterService extends RabbitDatasetService<ChecklistNormalizedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(ImporterService.class);

  private final ApplicationContext ctx;

  private final ImporterConfiguration cfg;
  private final DatasetImportService sqlService;
  private DatasetImportService solrService;
  private final NameUsageService nameUsageService;
  private final UsageService usageService;
  private final ZookeeperUtils zkUtils;

  public ImporterService(ImporterConfiguration cfg) {
    super("clb-importer", cfg.poolSize, cfg.messaging, cfg.ganglia, "import");
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

    ctx = SpringContextBuilder.create()
        .withClbConfiguration(cfg.clb)
        .withComponents(
            DatasetImportServiceMyBatis.class,
            UsageSyncServiceMyBatis.class,
            NameUsageServiceMyBatis.class,
            UsageServiceMyBatis.class,
            ParsedNameServiceMyBatis.class)
        .build();

    sqlService = ctx.getBean(DatasetImportServiceMyBatis.class);
    // TODO: 07/01/2022 configure solr?
//    solrService = null;
    nameUsageService = ctx.getBean(NameUsageServiceMyBatis.class);
    usageService = ctx.getBean(UsageServiceMyBatis.class);
  }

  @Override
  protected void process(ChecklistNormalizedMessage msg) throws Exception {
    try {
      Importer importer =
          Importer.create(
              cfg, msg.getDatasetUuid(), nameUsageService, usageService, sqlService, solrService);
      importer.run();

      // notify rabbit
      Date crawlFinished;
      if (cfg.zookeeper.isConfigured()) {
        crawlFinished = zkUtils.getDate(msg.getDatasetUuid(), ZookeeperUtils.FINISHED_CRAWLING);
        if (crawlFinished == null) {
          LOG.warn(
              "No crawlFinished date found in zookeeper, use current date instead for dataset {}",
              msg.getDatasetUuid());
          crawlFinished = new Date();
        }
      } else {
        crawlFinished = new Date();
      }

      send(
          new ChecklistSyncedMessage(
              msg.getDatasetUuid(),
              crawlFinished,
              importer.getSyncCounter(),
              importer.getDelCounter()));
      // finally delete artifacts unless configured not to or it is the nub!
      if (cfg.deleteNeo && !Constants.NUB_DATASET_KEY.equals(msg.getDatasetUuid())) {
        RegistryService.deleteStorageFiles(cfg.neo, msg.getDatasetUuid());
      }

    } finally {
      if (cfg.zookeeper.isConfigured()) {
        zkUtils.createOrUpdate(
            msg.getDatasetUuid(), ZookeeperUtils.PROCESS_STATE_CHECKLIST, ProcessState.FINISHED);
      }
    }
  }

  @Override
  protected void failed(UUID datasetKey) {
    if (cfg.zookeeper.isConfigured()) {
      zkUtils.createOrUpdate(datasetKey, ZookeeperUtils.FINISHED_REASON, FinishReason.ABORT);
    }
  }

  @Override
  protected void startUp() throws Exception {
    super.startUp();
  }

  @Override
  protected void shutDown() throws Exception {
    sqlService.close();
    solrService.close();
    super.shutDown();
  }

  @Override
  public Class<ChecklistNormalizedMessage> getMessageClass() {
    return ChecklistNormalizedMessage.class;
  }
}
