package org.gbif.checklistbank.cli.common;

import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.checklistbank.cli.normalizer.NoneMatchingService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWsClientModule;

import java.util.Properties;
import javax.validation.Valid;

import com.google.common.base.Strings;
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

  /**
   * The backbone matching URL to use.
   * If empty no backbone matching will be done.
   */
  @Valid
  public String matchWsUrl = null;

  public NameUsageMatchingService createMatchingService() {
    if (Strings.isNullOrEmpty(matchWsUrl)) {
      LOG.info("No species match service configured. Skip matching during normalization!");
      return new NoneMatchingService();

    } else {
      Properties props = new Properties();
      props.put("checklistbank.match.ws.url", matchWsUrl);
      // use ws clients for nub matching
      Injector injClient = Guice.createInjector(new ChecklistBankWsClientModule(props, false, true));
      LOG.info("Connecting to species match service at {}", matchWsUrl);
      return injClient.getInstance(NameUsageMatchingService.class);
    }
  }

}
