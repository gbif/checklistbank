package org.gbif.checklistbank.search;

import org.gbif.checklistbank.search.inject.SearchModule;

import java.util.Properties;

import org.apache.solr.client.solrj.SolrServer;

/**
 * Module for testing the search service.
 */
public class SearchTestModule extends SearchModule {

  private SolrServer solr;

  public SearchTestModule(Properties properties, SolrServer solr) {
    super(properties, false);
    this.solr = solr;
  }

  @Override
  protected void configureService() {
    bind(SolrServer.class).toInstance(solr);
    super.configureService();
  }
}
