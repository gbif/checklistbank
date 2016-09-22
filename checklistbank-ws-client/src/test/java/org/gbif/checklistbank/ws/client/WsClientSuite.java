package org.gbif.checklistbank.ws.client;

import org.gbif.checklistbank.ws.client.guice.ChecklistBankWsClientModule;
import org.gbif.checklistbank.ws.client.guice.ClbServer;
import org.gbif.checklistbank.ws.client.guice.UrlBindingModule;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
  DatasetMetricsWsClientIT.class,
  DescriptionWsClientIT.class,
  DistributionWsClientIT.class,
  IdentifierWsClientIT.class,
  MultimediaWsClientIT.class,
  NameUsageSearchWsClientIT.class,
  NameUsageWsClientIT.class,
  ReferenceWsClientIT.class,
  SpeciesProfileWsClientIT.class,
  TypeSpecimenWsClientIT.class,
  VernacularNameWsClientIT.class,
})
public class WsClientSuite {
  private static final Logger LOG = LoggerFactory.getLogger(WsClientSuite.class);
  public static final String PROPERTIES_FILE = "checklistbank.properties";

  //@Rule
  //public ClbDbTestRule dbSetup = ClbDbTestRule.squirrels();
  private static Server server;
  private static Injector clientInjector;

  @BeforeClass
  public static void setUp() {
    LOG.info("setting up server");

    try {
      server = ClbServer.buildServer(PropertiesUtil.loadProperties(PROPERTIES_FILE));
      LOG.info("Starting jetty at {}", server.getURI());
      server.start();

      clientInjector = Guice.createInjector(new UrlBindingModule(server.getURI(), "/"),
          new ChecklistBankWsClientModule(new Properties(), true, false));

    } catch (Exception e) {
      LOG.error("Failed to start clb webservice server", e);
      tearDown();
    }
  }

  public static <T> T getClient(Class<T> claszz) {
    return clientInjector.getInstance(claszz);
  }

  @AfterClass
  public static void tearDown() {
    if (server != null) {
      LOG.info("stop server");
      try {
        server.stop();
      } catch (Exception e) {
        LOG.error("Failed to shutdown clb webservice server", e);
      }
    }
  }
}
