package org.gbif.checklistbank.cli.importer;

import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.ZooKeeperConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.config.GangliaConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.common.search.inject.SolrConfig;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
@SuppressWarnings("PublicField")
public class ImporterConfiguration {

  @ParametersDelegate
  @Valid
  @NotNull
  public GangliaConfiguration ganglia = new GangliaConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public NeoConfiguration neo = new NeoConfiguration();

  @Parameter(names = "--deleteNeo")
  public boolean deleteNeo = true;

  @ParametersDelegate
  @NotNull
  @Valid
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public ClbConfiguration clb = new ClbConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public ZooKeeperConfiguration zookeeper = new ZooKeeperConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public SolrConfig solr = new SolrConfig();

  @Parameter(names = "--solr-sync-threads")
  @Min(1)
  public int solrSyncThreads = 1;

  @Parameter(names = "--pool-size")
  @Min(1)
  public int poolSize = 1;


  @Parameter(names = "--chunk-size")
  @Min(1)
  public int chunkSize = 10000;

  @Parameter(names = "--chunk-min-size")
  @Min(0)
  public int chunkMinSize = 100;
}
