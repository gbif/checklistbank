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
package org.gbif.checklistbank.cli.matcher;

import org.gbif.checklistbank.cli.common.RabbitDatasetService;
import org.gbif.checklistbank.cli.common.SpringContextBuilder;
import org.gbif.checklistbank.index.NameUsageIndexServiceEs;
import org.gbif.checklistbank.nub.lookup.DatasetMatchSummary;
import org.gbif.checklistbank.nub.lookup.NubMatchService;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.mybatis.service.*;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;
import org.gbif.common.messaging.api.messages.MatchDatasetMessage;
import org.gbif.nub.config.ClbNubConfiguration;
import org.gbif.nub.lookup.straight.DatasetMatchFailed;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.Timer;

public class MatcherService extends RabbitDatasetService<MatchDatasetMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(MatcherService.class);

  private final ApplicationContext ctx;

  private NubMatchService matcher;
  private static final String QUEUE = "clb-matcher";
  private final DatasetImportService sqlImportService;
  private final DatasetImportService solrImportService;
  private final MatcherConfiguration cfg;
  private Timer timer;

  public MatcherService(MatcherConfiguration cfg) {
    super(QUEUE, cfg.poolSize, cfg.messaging, cfg.ganglia, "match");
    this.cfg = cfg;

    ctx =
        SpringContextBuilder.create()
            .withClbConfiguration(cfg.clb)
            .withMessagingConfiguration(cfg.messaging)
            .withElasticsearchConfiguration(cfg.elasticsearch)
            .withApiUrl(cfg.apiUrl)
            .withComponents(
                DatasetImportServiceMyBatis.class,
                UsageSyncServiceMyBatis.class,
                NameUsageServiceMyBatis.class,
                UsageServiceMyBatis.class,
                ParsedNameServiceMyBatis.class,
                VernacularNameServiceMyBatis.class,
                DescriptionServiceMyBatis.class,
                DistributionServiceMyBatis.class,
                SpeciesProfileServiceMyBatis.class,
                CitationServiceMyBatis.class)
            .build();
    sqlImportService = ctx.getBean(DatasetImportServiceMyBatis.class);
    solrImportService = ctx.getBean(NameUsageIndexServiceEs.class);
  }

  @Override
  protected void initMetrics() {
    super.initMetrics();
    timer = getRegistry().timer("nub matcher process time");
  }

  @Override
  protected void startUpBeforeListening() throws Exception {
    // loads all nub usages directly from clb postgres - this can take a few minutes
    IdLookup lookup = IdLookupImpl.temp().load(ClbNubConfiguration.fromClbConfiguration(cfg.clb), false);
    matcher = new NubMatchService(cfg.clb, cfg.neo, lookup, sqlImportService, solrImportService);
  }

  @Override
  public Class<MatchDatasetMessage> getMessageClass() {
    return MatchDatasetMessage.class;
  }

  @Override
  protected void process(MatchDatasetMessage msg) throws Exception {
    final Timer.Context context = timer.time();
    try {
      LOG.info("Start matching dataset {}", msg.getDatasetUuid());
      DatasetMatchSummary summary = matcher.matchDataset(msg.getDatasetUuid());
      LOG.info("Dataset {} matched sucessfully: {}", msg.getDatasetUuid(), summary);
      // now also request new metrics from the analysis step
      //ChecklistSyncedMessage triggers a new dataset analysis
      send(new ChecklistSyncedMessage(msg.getDatasetUuid(), new Date(), 0, 0));

    } catch (DatasetMatchFailed e) {
      LOG.error("Dataset matching failed for {}", msg.getDatasetUuid(), e);
    }
    context.close();
  }

  @Override
  protected void shutDown() throws Exception {
    super.shutDown();
    sqlImportService.close();
    solrImportService.close();
  }
}
