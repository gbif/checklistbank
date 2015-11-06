package org.gbif.checklistbank.index.guice;

import org.gbif.checklistbank.index.NameUsageAvroExporter;
import org.gbif.checklistbank.index.NameUsageIndexingConfig;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.registry.ws.client.guice.RegistryWsClientModule;
import org.gbif.service.guice.PrivateServiceModule;
import org.gbif.ws.client.guice.AnonymousAuthModule;

import java.util.Properties;

/**
 * Guice module that initializes the required classes and dependencies for the CLB indexer.
 */
public class AvroIndexingModule extends PrivateServiceModule {


  public AvroIndexingModule(Properties properties) {
    super(NameUsageIndexingConfig.KEYS_INDEXING_CONF_PREFIX, properties);
  }

  @Override
  protected void configureService() {
    //installs the MyBatis service layer
    install(new ChecklistBankServiceMyBatisModule(getVerbatimProperties()));
    //installs auth module
    install(new AnonymousAuthModule());
    //installs registry client
    install(new RegistryWsClientModule(getVerbatimProperties()));

    expose(NameUsageAvroExporter.class);
  }

}
