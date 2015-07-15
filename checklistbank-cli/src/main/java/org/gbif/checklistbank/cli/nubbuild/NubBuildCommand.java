package org.gbif.checklistbank.cli.nubbuild;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.nub.NubBuilder;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;

import java.io.IOException;

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

  @Override
  protected void doRun() {
    DefaultMessagePublisher publisher = null;
    try {
      publisher = new DefaultMessagePublisher(cfg.messaging.getConnectionParameters());
      NubBuilder builder = NubBuilder.create(cfg);
      builder.run();
      LOG.info("Sending ChecklistNormalizedMessage for backbone");
      ChecklistNormalizedMessage msg = new ChecklistNormalizedMessage(Constants.NUB_DATASET_KEY);
      publisher.send(msg);

    } catch (IOException e) {
      LOG.error("Could not send ChecklistNormalizedMessage", e);
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);

    } finally {
      if (publisher != null) {
        publisher.close();
      }
    }
  }

}
