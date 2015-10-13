package org.gbif.checklistbank.index.guice;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;

/**
 * Simple wrapper class that can be used as a singleton to reference a exchangeable solr server.
 * The indexing tests need this so we can shutdown solr and recreate a new index without breaking existing
 * guice bindings.
 */
public class EmbeddedSolrReference {
  private EmbeddedSolrServer solr;

  public EmbeddedSolrReference(SolrClient solr) {
    this.solr = (EmbeddedSolrServer)solr;
  }

  public EmbeddedSolrServer getSolr() {
    return solr;
  }

  public void setSolr(EmbeddedSolrServer solr) {
    this.solr = solr;
  }
}
