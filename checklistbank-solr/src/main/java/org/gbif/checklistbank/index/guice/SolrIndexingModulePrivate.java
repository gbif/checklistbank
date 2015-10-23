package org.gbif.checklistbank.index.guice;

import org.gbif.checklistbank.index.NameUsageDocConverter;
import org.gbif.checklistbank.index.NameUsageIndexer;
import org.gbif.checklistbank.index.NameUsageIndexingConfig;
import org.gbif.common.search.inject.SolrModule;
import org.gbif.service.guice.PrivateServiceModule;

import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import org.apache.solr.client.solrj.SolrClient;


/**
 * Guice module that bind the indexing classes.
 * This module depends on the registry client and checklist bank mybatis modules to be installed too
 * which is done in the {@link SolrIndexingModule}.
 */
public class SolrIndexingModulePrivate extends PrivateServiceModule {

  public SolrIndexingModulePrivate(Properties properties) {
    super(NameUsageIndexingConfig.KEYS_INDEXING_CONF_PREFIX, properties);
  }

  @Override
  protected void configureService() {
    // Object <-> Document converter binding
    bind(NameUsageDocConverter.class).in(Scopes.SINGLETON);

    // Main indexer class
    bind(NameUsageIndexer.class).in(Scopes.SINGLETON);

    install(new SolrModule());

    expose(NameUsageIndexer.class);
    expose(SolrClient.class);
    expose(EmbeddedSolrReference.class);
  }

  @Inject
  @Singleton
  @Provides
  public EmbeddedSolrReference provideSolrReference(SolrClient solrClient) {
    return new EmbeddedSolrReference(solrClient);
  }
}
