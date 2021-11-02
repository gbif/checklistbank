package org.gbif.checklistbank.cli.matcher;

import com.google.common.util.concurrent.Service;
import org.gbif.cli.service.ServiceCommand;

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
