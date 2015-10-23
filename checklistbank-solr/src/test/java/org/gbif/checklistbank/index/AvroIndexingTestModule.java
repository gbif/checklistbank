/**
 *
 */
package org.gbif.checklistbank.index;

import org.gbif.checklistbank.index.guice.AvroIndexingModulePrivate;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;

import java.util.Properties;

import com.google.inject.AbstractModule;

/**
 * Guice module that initializes the required classes and dependencies for the CLB indexer
 * using a mocked dataset service.
 */
public class AvroIndexingTestModule extends AbstractModule {

  private final Properties properties;

  public AvroIndexingTestModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    // Installs the MyBatis service layer
    install(new ChecklistBankServiceMyBatisModule(properties));

    // Installs private indexing module
    install(new AvroIndexingModulePrivate(properties));
  }
}
