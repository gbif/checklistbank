package org.gbif.checklistbank.cli.common;

import java.io.File;
import java.util.UUID;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@SuppressWarnings("PublicField")
public class NeoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(NeoConfiguration.class);

  @NotNull
  @Parameter(names = "--neo-repo")
  public File neoRepository;

  @NotNull
  @Parameter(names = "--neo-batchsize")
  public int batchSize = 10000;

  @Parameter(names = "--neo-debug")
  public boolean debug = false;

  /**
   * none, soft, weak, strong
   * see http://docs.neo4j.org/chunked/stable/configuration-caches.html
   */
  @NotNull
  @Parameter(names = "--neo-cache-type")
  public String cacheType = "soft";

  @Min(0)
  @Parameter(names = "--neo-mapped-memory")
  public int mappedMemory = 256;

  public File neoDir(UUID datasetKey) {
    return new File(neoRepository, datasetKey.toString());
  }

  /**
   * Creates a new embedded db in the neoRepository folder.
   *
   * @param datasetKey subfolder for the db
   */
  public GraphDatabaseService newEmbeddedDb(UUID datasetKey) {
    Preconditions.checkNotNull(datasetKey);
    File storeDir = neoDir(datasetKey);
    if (storeDir.exists()) {
      // erase previous db
      LOG.info("Removing previous neo4j database from {}", storeDir.getAbsolutePath());
      FileUtils.deleteQuietly(storeDir);
    }
    GraphDatabaseFactory factory = new GraphDatabaseFactory();
    GraphDatabaseBuilder builder = factory.newEmbeddedDatabaseBuilder(storeDir.getAbsolutePath())
      .setConfig(GraphDatabaseSettings.keep_logical_logs, "false")
      .setConfig(GraphDatabaseSettings.cache_type, cacheType)
      .setConfig(GraphDatabaseSettings.pagecache_memory, mb(mappedMemory));
    if (debug) {
      builder
        .setConfig(GraphDatabaseSettings.dump_configuration, "true");
    }
    GraphDatabaseService db = builder.newGraphDatabase();
    LOG.info("Starting embedded neo4j database from {}", storeDir.getAbsolutePath());
    return db;
  }

  private String mb(int memoryInMB) {
    return memoryInMB + "M";
  }

}
