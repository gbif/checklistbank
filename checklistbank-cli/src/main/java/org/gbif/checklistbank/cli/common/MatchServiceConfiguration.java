package org.gbif.checklistbank.cli.common;

import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWsClientModule;

import java.util.Properties;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.ParametersDelegate;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration needed to connect to a species match service (nub lookup).
 */
@SuppressWarnings("PublicField")
public class MatchServiceConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(MatchServiceConfiguration.class);

  @ParametersDelegate
  @Valid
  @NotNull
  public String matchWsUrl = "http://api.gbif.org/v1/species/match";

  public NameUsageMatchingService createMatchingService() {
    // use ws clients for nub matching
    Injector injClient = Guice.createInjector(createMatchClientModule());
    return injClient.getInstance(NameUsageMatchingService.class);
  }

  public ChecklistBankWsClientModule createMatchClientModule() {
    LOG.info("Connecting to species match service at {}", matchWsUrl);
    Properties props = new Properties();
    props.put("checklistbank.match.ws.url", matchWsUrl);
    return new ChecklistBankWsClientModule(props, false, true);
  }


}
