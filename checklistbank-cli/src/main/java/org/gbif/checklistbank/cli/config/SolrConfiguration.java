package org.gbif.checklistbank.cli.config;

import org.gbif.common.search.solr.SolrConfig;
import org.gbif.common.search.solr.SolrServerType;

import javax.validation.constraints.Min;

import com.beust.jcommander.Parameter;

public class SolrConfiguration {

  @Parameter(names = {"--solr-server-type"})
  public SolrServerType serverType = SolrServerType.EMBEDDED;

  @Parameter(names = {"--solr-server-home"})
  public String serverHome;

  @Parameter(names = {"--solr-collection"})
  public String collection = "collection1";

  @Parameter(names = {"--solr-delete-on-exit"})
  public boolean deleteOnExit = false;

  @Parameter(names = {"--solr-id-field"})
  public String idField;

  @Parameter(names = {"--solr-sync-threads"})
  @Min(1)
  public int syncThreads = 1;

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
