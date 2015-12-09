package org.gbif.checklistbank.cli.shell;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import java.io.File;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import org.kohsuke.MetaInfServices;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.Settings;
import org.neo4j.shell.ShellSettings;

/**
 * Command that opens a neo4j shell server
 * ./neo4j-shell -port 8082
 */
@MetaInfServices(Command.class)
public class ShellCommand extends ServiceCommand {
  private final ShellConfiguration cfg = new ShellConfiguration();

  public ShellCommand() {
    super("shell");
  }

  @Override
  protected Object getConfigurationObject() {
    return cfg;
  }

  @Override
  protected Service getService() {
    return new NeoService();
  }

  private class NeoService extends AbstractIdleService {
    GraphDatabaseService neo;

    @Override
    protected void startUp() throws Exception {
      final File storeDir = cfg.neo.neoDir(cfg.key);
      Preconditions.checkArgument(storeDir.exists(), "No neo4j store directory existing at " + storeDir.getAbsolutePath());
      System.out.println("Connecting to neo4j store at " + storeDir.getAbsolutePath());

      neo = cfg.neo.newEmbeddedDb(storeDir, true, false)
          .setConfig(ShellSettings.remote_shell_enabled, Settings.TRUE)
          .setConfig(ShellSettings.remote_shell_port, String.valueOf(cfg.port))
          .newGraphDatabase();
    }

    @Override
    protected void shutDown() throws Exception {
      if (neo != null) {
        neo.shutdown();
      }
    }
  }

}
