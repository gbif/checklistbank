package org.gbif.checklistbank.index.backfill;

/**
 * Utility class that holds the common configuration keys for indexing modules.
 */
public class IndexingConfigKeys {

  //Default properties files
  public static final String CLB_PROPERTY_FILE = "checklistbank.properties";
  public static final String CLB_INDEXING_PROPERTY_TEST_FILE = "checklistbank-indexer-default.properties";

  public static final String KEYS_INDEXING_CONF_PREFIX = "checklistbank.indexer.";

  //Common indexing settings
  public static final String THREADS = "threads";
  public static final String BATCH_SIZE = "batchSize";
  public static final String WRITERS = "writers";
  public static final String LOG_INTERVAL = "logInterval";

  //Avro indexing settings
  public static final String NAME_NODE = "nameNode";
  public static final String TARGET_HDFS_DIR = "targetHdfsDir";


  private IndexingConfigKeys(){

  }

}
