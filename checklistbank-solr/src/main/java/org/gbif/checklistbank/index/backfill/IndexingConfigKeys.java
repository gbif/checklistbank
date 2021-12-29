/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
