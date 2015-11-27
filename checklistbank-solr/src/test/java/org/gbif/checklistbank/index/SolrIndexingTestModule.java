/**
 *
 */
package org.gbif.checklistbank.index;

import org.gbif.checklistbank.index.guice.SolrIndexingModulePrivate;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import com.google.common.base.Throwables;
import com.google.common.io.Resources;
import com.google.inject.AbstractModule;

/**
 * Guice module that initializes the required classes and dependencies for the CLB indexer
 * using a mocked dataset service.
 */
public class SolrIndexingTestModule extends AbstractModule {

  private final Properties properties;

  public SolrIndexingTestModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    removeUnUsedSolrCfg();
    // Installs the MyBatis service layer
    install(new ChecklistBankServiceMyBatisModule(properties));

    // Installs private indexing module
    install(new SolrIndexingModulePrivate(properties));
  }

  /**
   * Removes Solr config files NOT required to run the tests.
   */
  private static void removeUnUsedSolrCfg(){
    try {
      Files.delete(Paths.get(Resources.getResource("solr/collection1/conf/hdfs_directory_factory.xml").toURI()));
    } catch(URISyntaxException | IOException ex){
      Throwables.propagate(ex);
    }
  }
}
