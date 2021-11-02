package org.gbif.checklistbank.cli.registry;

import com.google.common.util.concurrent.Service;
import org.gbif.cli.service.ServiceCommand;

public class RegistryCommand extends ServiceCommand {

  private final RegistryConfiguration configuration = new RegistryConfiguration();

  public RegistryCommand() {
    super("registry");
  }

  @Override
  protected Service getService() {
    return new RegistryService(configuration);
  }

  @Override
  protected Object getConfigurationObject() {
    return configuration;
  }

}
