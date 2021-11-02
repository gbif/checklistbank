package org.gbif.checklistbank.cli.shell;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import org.gbif.cli.service.ServiceCommand;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;

/**
 * Command that opens an embedded neo4j with a bolt connector so it can be accessed remotely-
 * For example with the neo4j browser or the cypher-shell:
 * ./cypher-shell -u neo4j -p <password>
 */
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

      // shell command without bolt makes no sense
      cfg.neo.bolt = true;
      neo = cfg.neo.newEmbeddedDb(storeDir, false).newGraphDatabase();
      System.out.println("Opening neo4j bolt connector on port " + cfg.neo.port);
    }

    @Override
    protected void shutDown() throws Exception {
      if (neo != null) {
        neo.shutdown();
      }
    }
  }

}
