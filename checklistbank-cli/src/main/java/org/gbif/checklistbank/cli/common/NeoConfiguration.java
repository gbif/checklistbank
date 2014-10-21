package org.gbif.checklistbank.cli.common;

import java.io.File;
import java.util.UUID;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.google.common.base.Preconditions;
import org.neo4j.graphdb.GraphDatabaseService;
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

  @Min(0)
  @Parameter(names = "--neo-memory")
  public int memory = 0;

  @Parameter(names = "--neo-nodestore-memory")
  public int nodestoreMemory= 128;

  @Parameter(names = "--neo-propertystore-memory")
  public int propertystoreMemory = 64;

  @Parameter(names = "--neo-propertystore-index-keys-memory")
  public int propertystoreIndexKeysMemory = 64;

  @Parameter(names = "--neo-relationshipstore-memory")
  public int relationshipstoreMemory = 256;

  @Parameter(names = "--neo-memory-page")
  public int memoryPage = 64;

  @Parameter(names = "--neo-strings-memory")
  public int stringsMemory = 32;

  @Parameter(names = "--neo-arrays-memory")
  public int arraysMemory = 32;

  public File neoDir(UUID datasetKey) {
    return new File(neoRepository, datasetKey.toString());
  }

  /**
   * Creates a new emedded db in the neoRepository folder.
   *
   * @param datasetKey subfolder for the db
   */
  public GraphDatabaseService newEmbeddedDb(UUID datasetKey) {
    Preconditions.checkNotNull(datasetKey);
    File storeDir = neoDir(datasetKey);
    applyMemoryConfig();
    GraphDatabaseFactory factory = new GraphDatabaseFactory();
    GraphDatabaseService db = factory.newEmbeddedDatabaseBuilder(storeDir.getAbsolutePath())
      .setConfig(GraphDatabaseSettings.cache_type, "none")
      .setConfig(GraphDatabaseSettings.nodestore_mapped_memory_size, mb(nodestoreMemory))
      .setConfig(GraphDatabaseSettings.nodestore_propertystore_index_keys_mapped_memory_size, mb(propertystoreIndexKeysMemory))
      .setConfig(GraphDatabaseSettings.nodestore_propertystore_mapped_memory_size, mb(propertystoreMemory))
      .setConfig(GraphDatabaseSettings.relationshipstore_mapped_memory_size, mb(relationshipstoreMemory))
      .setConfig(GraphDatabaseSettings.mapped_memory_page_size, mb(memoryPage))
      .setConfig(GraphDatabaseSettings.strings_mapped_memory_size, mb(stringsMemory))
      .setConfig(GraphDatabaseSettings.arrays_mapped_memory_size, mb(arraysMemory))
      .newGraphDatabase();
    LOG.info("Starting embedded neo4j database from {}", storeDir.getAbsolutePath());
    return db;
  }

  private String mb(int memoryInMB) {
    return memoryInMB + "M";
  }

  /**
   * If the single total memory setting is configured use it to override the individual neo memory configurations.
   */
  private void applyMemoryConfig(){
    if (memory > 0) {
      final int sixteenth = memory / 16;
      nodestoreMemory = sixteenth * 3;
      relationshipstoreMemory = sixteenth*8;
      propertystoreMemory = sixteenth;
      propertystoreIndexKeysMemory = sixteenth;
      memoryPage = sixteenth;
      stringsMemory = sixteenth;
      arraysMemory = sixteenth;
    }
  }

}
