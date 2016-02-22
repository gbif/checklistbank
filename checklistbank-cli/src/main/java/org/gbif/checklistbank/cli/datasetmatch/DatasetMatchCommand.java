package org.gbif.checklistbank.cli.datasetmatch;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import com.google.common.util.concurrent.Service;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Command.class)
public class DatasetMatchCommand extends ServiceCommand {

  private final DatasetMatchConfiguration cfg = new DatasetMatchConfiguration();

  public DatasetMatchCommand() {
    super("dataset-matcher");
  }

  @Override
  protected Service getService() {
    return new DatasetMatchService(cfg);
  }

  @Override
  protected Object getConfigurationObject() {
    return cfg;
  }

}
