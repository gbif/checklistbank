package org.gbif.checklistbank.ws.client;

import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.gbif.checklistbank.ws.UrlBindingModule;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWsClientModule;
import org.gbif.checklistbank.ws.guice.ChecklistBankWsModule;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.client.BaseResourceTest;

import java.io.IOException;

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

  public static final String CONTEXT = "clb";
  protected static final String PROPERTIES_FILE = "checklistbank.properties";

  private Injector clientInjector;
  protected T wsClient;
  private final Class<T> wsClientClass;

  @Rule
  public ClbDbTestRule dbSetup = ClbDbTestRule.squirrels();

  public ClientMyBatisITBase(Class<T> wsClientClass) {
    super("org.gbif.checklistbank.ws", CONTEXT, ChecklistBankWsModule.class);
    this.wsClientClass = wsClientClass;
  }

  @Before
  public void init() throws IOException {
    clientInjector = Guice.createInjector(new UrlBindingModule(getBaseURI(), contextPath),
      new ChecklistBankWsClientModule(PropertiesUtil.loadProperties(PROPERTIES_FILE), true, false));
    wsClient = clientInjector.getInstance(wsClientClass);
  }
}
