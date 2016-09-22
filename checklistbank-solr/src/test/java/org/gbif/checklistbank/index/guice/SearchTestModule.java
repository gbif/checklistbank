package org.gbif.checklistbank.index.guice;

import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

/**
 * Module for testing the search service.
 */
public class SearchTestModule extends SearchModule {

  private SolrClient solrClient;

  public SearchTestModule(Properties properties, SolrClient solrClient) {
    super(properties, false);
    this.solrClient = solrClient;
  }

  @Override
  protected void configureService() {
    bind(SolrClient.class).toInstance(solrClient);
    super.configureService();
  }
}
