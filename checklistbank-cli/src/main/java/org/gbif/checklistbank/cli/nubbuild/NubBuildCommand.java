package org.gbif.checklistbank.cli.nubbuild;

import org.apache.commons.io.FileUtils;
import org.gbif.api.model.Constants;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.nub.NubBuilder;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;

import java.io.IOException;

import com.google.common.base.Throwables;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MetaInfServices(Command.class)
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
