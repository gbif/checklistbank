package org.gbif.checklistbank.ws.client;

import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;
import org.gbif.checklistbank.ws.UrlBindingModule;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWsClientModule;
import org.gbif.checklistbank.ws.guice.ChecklistBankWsModule;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.client.BaseResourceTest;

import java.io.IOException;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Rule;

/**
 * Base class for webservice client integration tests that access the mybatis based webservices.
 *
 * @param <T> the specific client to test.
 */
public class ClientMyBatisITBase<T> extends BaseResourceTest {

  protected static final String PROPERTIES_FILE = "checklistbank.properties";

  private Injector clientInjector;
  protected T wsClient;
  private final Class<T> wsClientClass;
  protected final UUID CHECKLIST_KEY_1001 = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");


  @Rule
  public DatabaseDrivenChecklistBankTestRule<NameUsageService> ddt =
    new DatabaseDrivenChecklistBankTestRule<NameUsageService>(NameUsageService.class);

  public ClientMyBatisITBase(Class<T> wsClientClass) {
    super("org.gbif.checklistbank.ws", "clb", ChecklistBankWsModule.class);
    this.wsClientClass = wsClientClass;
  }

  @Before
  public void init() throws IOException {
    clientInjector = Guice.createInjector(new UrlBindingModule(getBaseURI(), contextPath),
      new ChecklistBankWsClientModule(PropertiesUtil.loadProperties(PROPERTIES_FILE), false, true, false));
    wsClient = clientInjector.getInstance(wsClientClass);
  }
}
