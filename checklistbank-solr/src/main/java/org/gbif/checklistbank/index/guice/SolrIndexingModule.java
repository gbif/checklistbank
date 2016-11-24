package org.gbif.checklistbank.index.guice;

import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;

import java.util.Properties;

import com.google.inject.AbstractModule;


/**
 * Guice module that initializes the required classes and dependencies for the CLB indexer.
 */
public class SolrIndexingModule extends AbstractModule {

  private final Properties properties;

  public SolrIndexingModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    // Installs the MyBatis service layer
    install(new ChecklistBankServiceMyBatisModule(properties));

    // install registry client
    //install(new AnonymousAuthModule());
    //install(new RegistryWsClientModule(properties));

    // Installs private indexing module
    install(new SolrIndexingModulePrivate(properties));
  }
}
