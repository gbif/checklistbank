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
package org.gbif.checklistbank.cli.analysis;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.checklistbank.cli.common.RabbitDatasetService;
import org.gbif.checklistbank.cli.common.SpringContextBuilder;
import org.gbif.checklistbank.service.DatasetAnalysisService;
import org.gbif.checklistbank.service.mybatis.service.DatasetAnalysisServiceMyBatis;
import org.gbif.common.messaging.api.messages.BackboneChangedMessage;
import org.gbif.common.messaging.api.messages.ChecklistAnalyzedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class AnalysisService extends RabbitDatasetService<ChecklistSyncedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(AnalysisService.class);

  private final ApplicationContext ctx;

  private final DatasetAnalysisService analysisService;

  public AnalysisService(AnalysisConfiguration cfg) {
    super("clb-analysis", cfg.poolSize, cfg.messaging, cfg.ganglia, "analyze");
    ctx = SpringContextBuilder.create()
        .withComponents(DatasetAnalysisServiceMyBatis.class)
        .build();

    analysisService = ctx.getBean(DatasetAnalysisServiceMyBatis.class);
  }

  @Override
  public Class<ChecklistSyncedMessage> getMessageClass() {
    return ChecklistSyncedMessage.class;
  }

  @Override
  protected void process(ChecklistSyncedMessage msg) throws IOException {
    DatasetMetrics metrics = analysisService.analyse(msg.getDatasetUuid(), msg.getCrawlFinished());

    send(new ChecklistAnalyzedMessage(msg.getDatasetUuid()));

    if (Constants.NUB_DATASET_KEY.equals(msg.getDatasetUuid())) {
      send(new BackboneChangedMessage(metrics));
    }
  }
}
