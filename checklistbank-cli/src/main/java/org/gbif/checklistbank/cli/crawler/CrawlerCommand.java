package org.gbif.checklistbank.cli.crawler;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import com.google.common.util.concurrent.Service;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Command.class)
public class CrawlerCommand extends ServiceCommand {

  private final CrawlerConfiguration configuration = new CrawlerConfiguration();

  public CrawlerCommand() {
    super("crawler");
  }

  @Override
  protected Service getService() {
    return new CrawlerService(configuration);
  }

  @Override
  protected Object getConfigurationObject() {
    return configuration;
  }

}
