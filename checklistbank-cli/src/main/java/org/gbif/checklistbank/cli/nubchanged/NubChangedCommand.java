package org.gbif.checklistbank.cli.nubchanged;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import com.google.common.util.concurrent.Service;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Command.class)
public class NubChangedCommand extends ServiceCommand {

  private final NubChangedConfiguration configuration = new NubChangedConfiguration();

  public NubChangedCommand() {
    super("nub-changed");
  }

  @Override
  protected Service getService() {
    return new NubChangedService(configuration);
  }

  @Override
  protected Object getConfigurationObject() {
    return configuration;
  }

}
