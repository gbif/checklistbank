package org.gbif.checklistbank.cli.config;

import org.gbif.common.search.solr.SolrConfig;
import org.gbif.common.search.solr.SolrServerType;

import com.beust.jcommander.Parameter;

public class SolrConfiguration {

  @Parameter(names = {"--solr-server-type"})
  private SolrServerType serverType = SolrServerType.EMBEDDED;

  @Parameter(names = {"--solr-server-home"})
  private String serverHome;

  @Parameter(names = {"--solr-collection"})
  private String collection = "collection1";

  @Parameter(names = {"--solr-delete-on-exit"})
  private boolean deleteOnExit = false;

  @Parameter(names = {"--solr-id-field"})
  private String idField;

  public SolrConfig toSolrConfig() {
    SolrConfig solrConfig = new SolrConfig();
    solrConfig.setServerType(serverType);
    solrConfig.setServerHome(serverHome);
    solrConfig.setCollection(collection);
    solrConfig.setDeleteOnExit(deleteOnExit);
    solrConfig.setIdField(idField);

    return solrConfig;
  }
}
