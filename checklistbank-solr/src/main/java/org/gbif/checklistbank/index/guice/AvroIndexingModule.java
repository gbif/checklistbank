package org.gbif.checklistbank.index.guice;

import org.gbif.checklistbank.index.backfill.AvroExporter;
import org.gbif.checklistbank.index.backfill.IndexingConfigKeys;
import org.gbif.service.guice.PrivateServiceModule;

import java.util.Properties;

import com.google.inject.AbstractModule;

/**
 * Guice module that initializes the required classes and dependencies for the CLB indexer.
 */
public class AvroIndexingModule extends AbstractModule {

  private final Properties properties;

  public AvroIndexingModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  protected void configure() {
    //installs the Avro indexing module
    install(new AvroIndexingInternalModule(properties));
  }

  /**
   * Internal module to isolate
   */
  private static class AvroIndexingInternalModule extends PrivateServiceModule {

    public AvroIndexingInternalModule(Properties properties){
      super(IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX,properties);
    }

    @Override
    public void configureService() {
      //installs the MyBatis service layer
      //install(new ChecklistBankServiceMyBatisModule(getVerbatimProperties()));

      //installs registry client
      //install(new AnonymousAuthModule());
      //install(new RegistryWsClientModule(getVerbatimProperties()));

      bind(AvroExporter.class);
      expose(AvroExporter.class);
    }
  }
}
