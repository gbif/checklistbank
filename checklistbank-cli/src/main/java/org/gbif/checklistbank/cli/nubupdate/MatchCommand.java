package org.gbif.checklistbank.cli.nubupdate;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import com.google.common.util.concurrent.Service;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Command.class)
public class MatchCommand extends ServiceCommand {

  private final MatchConfiguration configuration = new MatchConfiguration();

  public MatchCommand() {
    super("nubmatcher");
  }

  @Override
  protected Service getService() {
    return new MatchService(configuration);
  }

  @Override
  protected Object getConfigurationObject() {
    return configuration;
  }

}
