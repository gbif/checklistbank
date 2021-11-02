package org.gbif.checklistbank.cli.crawler;

import com.google.common.util.concurrent.Service;
import org.gbif.cli.service.ServiceCommand;

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
