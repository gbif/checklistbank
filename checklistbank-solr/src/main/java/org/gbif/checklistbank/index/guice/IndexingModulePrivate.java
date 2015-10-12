package org.gbif.checklistbank.index.guice;

import org.gbif.checklistbank.index.NameUsageAvroExporter;
import org.gbif.checklistbank.index.NameUsageDocConverter;
import org.gbif.checklistbank.index.NameUsageIndexer;
import org.gbif.common.search.inject.SolrModule;
import org.gbif.service.guice.PrivateServiceModule;

import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import org.apache.solr.client.solrj.SolrServer;


/**
 * Guice module that bind the indexing classes.
 * This module depends on the registry client and checklist bank mybatis modules to be installed too
 * which is done in the {@link IndexingModule}.
 */
public class IndexingModulePrivate extends PrivateServiceModule {

  private static final String PREFIX = "checklistbank.indexer.";

  public IndexingModulePrivate(Properties properties) {
    super(PREFIX, properties);
  }

  @Override
  protected void configureService() {
    // Object <-> Document converter binding
    bind(NameUsageDocConverter.class).in(Scopes.SINGLETON);

    // Main indexer class
    bind(NameUsageIndexer.class).in(Scopes.SINGLETON);

    // Main indexer class
    bind(NameUsageAvroExporter.class).in(Scopes.SINGLETON);

    install(new SolrModule());

    expose(NameUsageIndexer.class);
    expose(NameUsageAvroExporter.class);
    expose(SolrServer.class);
    expose(EmbeddedSolrReference.class);
  }

  @Inject
  @Singleton
  @Provides
  public EmbeddedSolrReference provideSolrReference(SolrServer solr) {
    return new EmbeddedSolrReference(solr);
  }
}
