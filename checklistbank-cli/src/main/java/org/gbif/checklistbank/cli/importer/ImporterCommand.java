package org.gbif.checklistbank.cli.importer;

import com.google.common.util.concurrent.Service;
import org.gbif.cli.service.ServiceCommand;

public class ImporterCommand extends ServiceCommand {

  private final ImporterConfiguration configuration = new ImporterConfiguration();

  public ImporterCommand() {
    super("importer");
  }

  @Override
  protected Service getService() {
    return new ImporterService(configuration);
  }

  @Override
  protected Object getConfigurationObject() {
    return configuration;
  }

}
