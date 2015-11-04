package org.gbif.checklistbank.cli.registry;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import com.google.common.util.concurrent.Service;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Command.class)
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
