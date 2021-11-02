package org.gbif.checklistbank.cli.analysis;

import com.google.common.util.concurrent.Service;
import org.gbif.cli.service.ServiceCommand;

public class AnalysisCommand extends ServiceCommand {

  private final AnalysisConfiguration configuration = new AnalysisConfiguration();

  public AnalysisCommand() {
    super("analysis");
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
