package org.gbif.checklistbank.index.guice;

import org.gbif.checklistbank.index.NameUsageIndexServicePassThru;
import org.gbif.checklistbank.index.NameUsageIndexServiceSolr;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.common.search.inject.SolrConfig;
import org.gbif.common.search.inject.SolrModule;
import org.gbif.common.search.solr.SolrServerType;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice module that binds a NameUsageSolrService expecting the mybatis module to be available already.
 */
public class RealTimeModule extends PrivateModule {
  private static Logger LOG = LoggerFactory.getLogger(RealTimeModule.class);
  private final SolrConfig cfg;
  private final int syncThreads = 2;;

  public RealTimeModule(SolrConfig cfg) {
    this.cfg = cfg;
  }

  @Override
  protected void configure() {
    if (cfg.serverType == null
            || cfg.serverHome == null
            || (cfg.serverType == SolrServerType.HTTP && !cfg.serverHome.startsWith("http"))
      ) {
      bind(DatasetImportService.class)
          .annotatedWith(Solr.class)
          .to(NameUsageIndexServicePassThru.class)
          .in(Scopes.SINGLETON);
      LOG.info("No solr service configured. Using pass thru mock solr service.");
    } else {
      SolrModule mod = new SolrModule();
      bind(SolrClient.class).toInstance(mod.providerSolr(cfg));
      bind(Integer.class)
          .annotatedWith(Solr.class)
          .toInstance(syncThreads);
      bind(DatasetImportService.class)
          .annotatedWith(Solr.class)
          .to(NameUsageIndexServiceSolr.class)
          .in(Scopes.SINGLETON);
    }
    expose(DatasetImportService.class).annotatedWith(Solr.class);
  }
}
