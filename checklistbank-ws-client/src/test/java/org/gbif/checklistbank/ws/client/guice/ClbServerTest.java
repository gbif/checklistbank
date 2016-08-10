package org.gbif.checklistbank.ws.client.guice;

import org.gbif.checklistbank.ws.client.JunitExecutionOrderSuite;
import org.gbif.checklistbank.ws.client.WsClientSuite;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class ClbServerTest {
  private static final Logger LOG = LoggerFactory.getLogger(JunitExecutionOrderSuite.class);
  private static Injector clientInjector;
  private static Server server;

  @BeforeClass
  public static void setUp() {
    LOG.info("setting up server");

    try {
      server = ClbServer.buildServer(PropertiesUtil.loadProperties(WsClientSuite.PROPERTIES_FILE));
      LOG.info("Starting jetty at {}", server.getURI());
      server.start();

      clientInjector = Guice.createInjector(
          new UrlBindingModule(server.getURI(), "/"),
          new ChecklistBankWsClientModule(new Properties(), true, false)
      );

    } catch (Exception e) {
      LOG.error("Failed to start clb webservice server", e);
      tearDown();
    }
  }

  @Test
  public void testBuildServer() throws Exception {
    assertNotNull(server);
    assertNotNull(clientInjector);
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