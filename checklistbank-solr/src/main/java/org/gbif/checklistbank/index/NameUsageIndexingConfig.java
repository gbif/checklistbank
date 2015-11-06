package org.gbif.checklistbank.index;

/**
 * Utility class that holds the common configuration keys for indexing modules.
 */
public class NameUsageIndexingConfig {

  //Default properties files
  public static final String CLB_PROPERTY_FILE = "checklistbank.properties";
  public static final String CLB_INDEXING_PROPERTY_TEST_FILE = "checklistbank-indexer-default.properties";

  public static final String KEYS_INDEXING_CONF_PREFIX = "checklistbank.indexer.";

  //Common indexing settings
  public static final String THREADS = KEYS_INDEXING_CONF_PREFIX + "threads";
  public static final String BATCH_SIZE = KEYS_INDEXING_CONF_PREFIX + "batchSize";
  public static final String WRITERS = KEYS_INDEXING_CONF_PREFIX + "writers";
  public static final String LOG_INTERVAL = KEYS_INDEXING_CONF_PREFIX + "logInterval";

  //Avro indexing settings
  public static final String NAME_NODE = KEYS_INDEXING_CONF_PREFIX + "nameNode";
  public static final String TARGET_HDFS_DIR = KEYS_INDEXING_CONF_PREFIX + "targetHdfsDir";


  private NameUsageIndexingConfig(){

  }

}
