package org.gbif.checklistbank.cli.common;

import com.beust.jcommander.Parameter;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.logging.slf4j.Slf4jLogProvider;
import org.neo4j.shell.ShellSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.UUID;

/**
 *
 */
public class NeoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(NeoConfiguration.class);

  @NotNull
  @Parameter(names = "--neo-repo")
  public File neoRepository;

  @NotNull
  @Parameter(names = "--neo-batchsize")
  public int batchSize = 10000;

  @Min(0)
  @Parameter(names = "--neo-mapped-memory")
  public int mappedMemory = 128;

  @Min(1000)
  @Parameter(names = {"--neo-shell-port"}, required = false)
  public int port = 1337;

  @Parameter(names = {"--neo-shell"}, required = false)
  public boolean shell = false;

  public File neoDir(UUID datasetKey) {
    return new File(neoRepository, datasetKey.toString());
  }

  /**
   * @return the KVP dbmap file used for the given dataset
   */
  public File kvp(UUID datasetKey) {
    return new File(neoRepository, "kvp" + File.separator + datasetKey.toString());
  }

  /**
   * Creates a new embedded db in the neoRepository folder.
   *
   * @param eraseExisting if true deletes previously existing db
   */
  public GraphDatabaseBuilder newEmbeddedDb(File storeDir, boolean eraseExisting) {
    if (eraseExisting && storeDir.exists()) {
      // erase previous db
      LOG.debug("Removing previous neo4j database from {}", storeDir.getAbsolutePath());
      FileUtils.deleteQuietly(storeDir);
    }
    GraphDatabaseBuilder builder = new GraphDatabaseFactory()
        .setUserLogProvider(new Slf4jLogProvider())
        .newEmbeddedDatabaseBuilder(storeDir)
        .setConfig(GraphDatabaseSettings.keep_logical_logs, "false")
        .setConfig(GraphDatabaseSettings.allow_store_upgrade, "true")
        .setConfig(GraphDatabaseSettings.pagecache_memory, mappedMemory + "M");
    if (shell) {
      LOG.info("Enable neo4j shell on port " + port);
      builder.setConfig(ShellSettings.remote_shell_enabled, "true")
          .setConfig(ShellSettings.remote_shell_port, String.valueOf(port))
          // listen to all IPs, not localhost only
          .setConfig(ShellSettings.remote_shell_host, "0.0.0.0");
    }
    return builder;
  }

}


