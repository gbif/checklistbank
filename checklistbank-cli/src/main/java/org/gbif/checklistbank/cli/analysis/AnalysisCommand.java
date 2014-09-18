package org.gbif.checklistbank.cli.analysis;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import com.google.common.util.concurrent.Service;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Command.class)
public class AnalysisCommand extends ServiceCommand {

  private final AnalysisConfiguration configuration = new AnalysisConfiguration();

  public AnalysisCommand() {
    super("matcher");
  }

  @Override
  protected Service getService() {
    return new AnalysisService(configuration);
  }

  @Override
  protected Object getConfigurationObject() {
    return configuration;
  }

}
