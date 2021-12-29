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
package org.gbif.checklistbank.index;

import org.gbif.checklistbank.index.guice.SolrIndexingModulePrivate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;
import com.google.inject.AbstractModule;

/**
 * Guice module that initializes the required classes and dependencies for the CLB indexer
 * using a mocked dataset service.
 */
public class SolrIndexingTestModule extends AbstractModule {
  private static final Logger LOG = LoggerFactory.getLogger(SolrIndexingTestModule.class);

  private final Properties properties;

  public SolrIndexingTestModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    removeUnUsedSolrCfg();
    // Installs the MyBatis service layer
//    install(new ChecklistBankServiceMyBatisModule(properties));

    // Installs private indexing module
    install(new SolrIndexingModulePrivate(properties));
  }

  /**
   * Removes Solr config files NOT required to run the tests.
   */
  private static void removeUnUsedSolrCfg(){
    try {
      Files.delete(Paths.get(Resources.getResource("solr/checklistbank/conf/hdfs_directory_factory.xml").toURI()));
      LOG.info("Removed hdfs_directory_factory.xml from solr resources.");
    } catch(Exception e){
      // already removed
      LOG.warn("Failed to remove hdfs_directory_factory.xml from solr resources. Probably has been removed before", e.getMessage());
    }
  }
}
