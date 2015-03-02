package org.gbif.checklistbank.cli.deletion;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import com.google.common.util.concurrent.Service;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Command.class)
public class DeleteCommand extends ServiceCommand {

  private final DeleteConfiguration configuration = new DeleteConfiguration();

  public DeleteCommand() {
    super("deletion");
  }

  @Override
  protected Service getService() {
    return new DeleteService(configuration);
  }

  @Override
  protected Object getConfigurationObject() {
    return configuration;
  }

}
