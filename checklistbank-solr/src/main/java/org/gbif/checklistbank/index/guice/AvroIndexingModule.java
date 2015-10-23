package org.gbif.checklistbank.index.guice;

import org.gbif.checklistbank.index.NameUsageAvroExporter;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.registry.ws.client.guice.RegistryWsClientModule;
import org.gbif.ws.client.guice.AnonymousAuthModule;

import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Scope;
import com.google.inject.Scopes;

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
    // Installs the MyBatis service layer
    install(new ChecklistBankServiceMyBatisModule(properties));

    // install registry client
    install(new AnonymousAuthModule());
    install(new RegistryWsClientModule(properties));

    //expose the Avro exporter
    bind(NameUsageAvroExporter.class).in(Scopes.SINGLETON);
  }
}
