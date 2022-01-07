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
package org.gbif.checklistbank.cli.common;

import java.io.File;
import java.util.UUID;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.kernel.configuration.BoltConnector;
import org.neo4j.kernel.configuration.Settings;
import org.neo4j.logging.slf4j.Slf4jLogProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(NeoConfiguration.class);

  @NotNull
  public File neoRepository = new File("/tmp/neo");

  @NotNull
  public int batchSize = 10000;

  @Min(0)
  public int mappedMemory = 128;

  @Min(1000)
  public int port = 7687;

  public boolean bolt = false;

  public File neoDir(UUID datasetKey) {
    return new File(neoRepository, datasetKey.toString() + File.separator + "db");
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
        .setConfig(GraphDatabaseSettings.pagecache_memory, mappedMemory + "m");
    if (bolt) {
      LOG.info("Enable neo4j bolt protocol on port " + port);
      BoltConnector bolt = new BoltConnector("bolt-shell");
      builder
          .setConfig( bolt.type, "BOLT" )
          .setConfig( bolt.enabled, Settings.TRUE)
          .setConfig( bolt.listen_address, "0.0.0.0:"+port)
          .setConfig(bolt.encryption_level, BoltConnector.EncryptionLevel.OPTIONAL.toString());
    }
    return builder;
  }

}
