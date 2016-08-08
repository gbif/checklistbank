package org.gbif.checklistbank.index.guice;

import org.gbif.checklistbank.index.backfill.AvroExporter;
import org.gbif.checklistbank.index.backfill.IndexingConfigKeys;
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
    super(IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX, properties);
  }

  @Override
  protected void configureService() {
    //expose the Avro exporter
    bind(AvroExporter.class).in(Scopes.SINGLETON);

    expose(AvroExporter.class);
  }
}
