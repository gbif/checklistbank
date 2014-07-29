package org.gbif.checklistbank.cli.admin;

import org.gbif.api.model.crawler.DwcaValidationReport;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.cli.postal.ChecklistNormalizedMessage;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.DwcaMetasyncFinishedMessage;
import org.gbif.common.messaging.api.messages.StartCrawlMessage;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import com.google.common.collect.Maps;
import org.kohsuke.MetaInfServices;

/**
 * Command that issues new normalize or import messages for manual admin purposes.
 */
@MetaInfServices(Command.class)
public class AdminCommand extends BaseCommand {

  private final AdminConfiguration cfg = new AdminConfiguration();

  public AdminCommand() {
    super("admin");
  }

  @Override
  protected Object getConfigurationObject() {
    return cfg;
  }

  @Override
  protected void doRun() {
    try {
      MessagePublisher publisher = new DefaultMessagePublisher(cfg.messaging.getConnectionParameters());
      switch (cfg.operation) {
        case DOWNLOAD:
          publisher.send( new StartCrawlMessage(cfg.datasetKey));

        case NORMALIZE:
          // validation result is fake as it is unused in checklist processing (so far)
          publisher.send( new DwcaMetasyncFinishedMessage(cfg.datasetKey, DatasetType.CHECKLIST,
                                                          URI.create("http://fake.org"), 1, Maps.<String, UUID>newHashMap(),
                                                          new DwcaValidationReport(cfg.datasetKey, 1, 1, 0, 1, 0, true)) );
          break;
        case IMPORT:
          publisher.send( new ChecklistNormalizedMessage(cfg.datasetKey) );
          break;

        default:
          throw new UnsupportedOperationException();
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

}
