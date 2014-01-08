package org.gbif.checklistbank.ws.client.guice;

import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.service.checklistbank.NameUsageSearchService;

import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;

public class ChecklistBankWsClientModuleTest {

  private static final Properties properties = new Properties();

  static {
    properties.setProperty("registry.ws.url", "http://test.gbif.org");
    properties.setProperty("occurrencestore.ws.url", "http://test.gbif.org");
    properties.setProperty("checklistbank.ws.url", "http://test.gbif.org");
    properties.setProperty("checklistbank.search.ws.url", "http://test.gbif.org");
    properties.setProperty("checklistbank.match.ws.url", "http://test.gbif.org");
  }

  @Test
  public void testFullModule() {
    Injector inj = Guice.createInjector(new ChecklistBankWsClientModule(properties, true, true, true));

    inj.getInstance(NameUsageSearchService.class);
    inj.getInstance(DescriptionService.class);
  }

  @Test
  public void testSearchModule() {
    Injector inj = Guice.createInjector(new ChecklistBankWsClientModule(properties, true, false, false));
    inj.getInstance(NameUsageSearchService.class);
  }

  @Test
  public void testServiceModule() {
    Injector inj = Guice.createInjector(new ChecklistBankWsClientModule(properties, false, true, false));
    inj.getInstance(DescriptionService.class);
  }

  @Test
  public void testMatchingModule() {
    Injector inj = Guice.createInjector(new ChecklistBankWsClientModule(properties, false, false, true));
    inj.getInstance(NameUsageMatchingService.class);
  }
}
