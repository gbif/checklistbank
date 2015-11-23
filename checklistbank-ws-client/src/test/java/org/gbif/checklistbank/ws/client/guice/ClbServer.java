package org.gbif.checklistbank.ws.client.guice;

import org.gbif.checklistbank.ws.guice.ChecklistBankWsModule;

import java.util.Properties;

import com.google.inject.servlet.GuiceFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 *
 */
public class ClbServer {
  private final static String SYSTEM_PROPERTY_TEST_PORT = "jetty.port";

  public static Server buildServer(Properties clbProps) {
    final Server server = new Server(determinePort());
    final ChecklistBankWsModule clbWs = new ChecklistBankSearchWsTestModule(clbProps);

    final ServletContextHandler root=new ServletContextHandler(server, "/", ServletContextHandler.NO_SESSIONS);
    root.addEventListener(clbWs);
    root.addFilter(GuiceFilter.class, "/*", null);
    root.addServlet(DefaultServlet.class, "/");
    server.setStopAtShutdown(true);

    return server;
  }

  private static int determinePort() {
    String port = System.getProperty(SYSTEM_PROPERTY_TEST_PORT);
    if (port != null) {
      return Integer.parseInt(port);
    }
    return 8080;
  }

}
