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
package org.gbif.checklistbank.cli.nubbuild;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.nub.NubBuilder;
import org.gbif.cli.BaseCommand;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public class NubBuildCommand extends BaseCommand {
  private static final Logger LOG = LoggerFactory.getLogger(NubBuildCommand.class);
  private final NubConfiguration cfg = new NubConfiguration();

  public NubBuildCommand() {
    super("nub-build");
  }

  @Override
  protected Object getConfigurationObject() {
    return cfg;
  }

  private void cleanRepo(NeoConfiguration cfg) {
    if (cfg.neoRepository.exists()) {
      LOG.info("Clean neo repositories in {}", cfg.neoRepository);
      try {
        FileUtils.cleanDirectory(cfg.neoRepository);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to clean neo repository", e);
      }
    }
  }

  @Override
  protected void doRun() {
    cleanRepo(cfg.neo);
    cleanRepo(cfg.neoSources);

    NubBuilder builder = NubBuilder.create(cfg);
    builder.run();
    builder.report(cfg.reportDir);

    if (cfg.autoImport) {
      try {
        MessagePublisher publisher = new DefaultMessagePublisher(cfg.messaging.getConnectionParameters());
        publisher.send(new ChecklistNormalizedMessage(Constants.NUB_DATASET_KEY));
        LOG.info("Sending ChecklistNormalizedMessage for backbone dataset {}", Constants.NUB_DATASET_KEY);
        publisher.close();
      } catch (IOException e) {
        Throwables.propagate(e);
      }
    }
  }

}
