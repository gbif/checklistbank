package org.gbif.checklistbank.cli.shell;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;
import org.kohsuke.MetaInfServices;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;

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

      // shell command without shell makes no sense
      if (!cfg.neo.shell) {
        cfg.neo.shell = true;
      }
      neo = cfg.neo.newEmbeddedDb(storeDir, false).newGraphDatabase();
      System.out.println("Opening neo4j shell on port " + cfg.neo.port);
    }

    @Override
    protected void shutDown() throws Exception {
      if (neo != null) {
        neo.shutdown();
      }
    }
  }

}
