package org.gbif.checklistbank.index.backfill;

import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.index.NameUsageDocConverter;
import org.gbif.checklistbank.index.guice.SolrIndexingModule;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.service.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.VernacularNameServiceMyBatis;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checklist Bank multithreaded name usage solr indexer.
 * This class creates a pool of configurable <i>threads</i> that concurrently execute a number of jobs
 * each processing a configurable number of name usages (<i>batchSize</i>)
 * using a configurable number of concurrent lucene <i>writers</i>.
 * The indexer makes direct use of the mybatis layer and requires a checklist bank datasource to be configured.
 */
public class SolrBackfill extends NameUsageBatchProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(SolrBackfill.class);

  private final int numWriters;

  // other injected instances
  private NameUsageDocConverter solrDocumentConverter;
  private final SolrClient solr;


  @Inject
  public SolrBackfill(SolrClient solr,
                      @Named(IndexingConfigKeys.THREADS) Integer threads,
                      @Named(IndexingConfigKeys.BATCH_SIZE) Integer batchSize,
                      @Named(IndexingConfigKeys.WRITERS) Integer numWriters,
                      @Named(IndexingConfigKeys.LOG_INTERVAL) Integer logInterval,
                      UsageService nameUsageService, NameUsageDocConverter solrDocumentConverter,
                      VernacularNameService vernacularNameService, DescriptionService descriptionService,
                      DistributionService distributionService, SpeciesProfileService speciesProfileService) {

    super(threads, batchSize, logInterval, nameUsageService, vernacularNameService, descriptionService, distributionService, speciesProfileService);
    this.numWriters = numWriters;
    this.solrDocumentConverter = solrDocumentConverter;
    // final solr
    this.solr = solr;
  }

  /**
   * Entry point for execution.
   * Commandline arguments are:
   * 0: required path to property file
   */
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      throw new IllegalArgumentException("Path to property file required");
    }
    // Creates the injector
    Properties props = loadProperties(args[0]);
    Injector injector = Guice.createInjector(new SolrIndexingModule(props));
    // Gets the indexer instance
    SolrBackfill nameUsageIndexer = injector.getInstance(SolrBackfill.class);
    nameUsageIndexer.run();
    // This statement is used because the Guice container is not stopped inside the threadpool.
    System.exit(0);
  }

  @Override
  protected void shutdownService(int tasksCount) {
    super.shutdownService(tasksCount);
    // commit solr
    try {
      solr.commit();
      LOG.info("Solr server committed. Indexing completed!");
    } catch (SolrServerException | IOException e) {
      LOG.error("Error committing solr", e);
    }
  }

  @Override
  protected Callable<Integer> newBatchJob(int startKey, int endKey, UsageService nameUsageService, VernacularNameServiceMyBatis vernacularNameService, DescriptionServiceMyBatis descriptionService, DistributionServiceMyBatis distributionService, SpeciesProfileServiceMyBatis speciesProfileService) {
    return new NameUsageIndexingJob(solr, nameUsageService, startKey, endKey, solrDocumentConverter,
        vernacularNameService, descriptionService, distributionService, speciesProfileService);
  }

}
