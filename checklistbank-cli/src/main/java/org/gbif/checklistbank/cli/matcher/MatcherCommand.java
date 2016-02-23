package org.gbif.checklistbank.cli.matcher;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import com.google.common.util.concurrent.Service;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Command.class)
public class MatcherCommand extends ServiceCommand {

  private final MatcherConfiguration cfg = new MatcherConfiguration();

  public MatcherCommand() {
    super("dataset-matcher");
  }

  @Override
  protected Service getService() {
    return new MatcherService(cfg);
  }

  @Override
  protected Object getConfigurationObject() {
    return cfg;
  }

}
