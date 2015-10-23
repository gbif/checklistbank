package org.gbif.checklistbank.index.guice;

import org.gbif.checklistbank.index.NameUsageAvroExporter;
import org.gbif.checklistbank.index.NameUsageIndexingConfig;
import org.gbif.service.guice.PrivateServiceModule;

import java.util.Properties;

import com.google.inject.Scopes;
/**
 * Guice module that bind the indexing classes.
 * This module depends on the registry client and checklist bank mybatis modules to be installed too
 * which is done in the {@link org.gbif.checklistbank.index.guice.SolrIndexingModule}.
 */
public class AvroIndexingModulePrivate extends PrivateServiceModule {

  public AvroIndexingModulePrivate(Properties properties) {
    super(NameUsageIndexingConfig.KEYS_INDEXING_CONF_PREFIX, properties);
  }

  @Override
  protected void configureService() {
    //expose the Avro exporter
    bind(NameUsageAvroExporter.class).in(Scopes.SINGLETON);

    expose(NameUsageAvroExporter.class);
  }
}
