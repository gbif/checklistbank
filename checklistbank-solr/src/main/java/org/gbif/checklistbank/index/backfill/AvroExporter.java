package org.gbif.checklistbank.index.backfill;

import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.index.guice.AvroIndexingModule;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.VernacularNameServiceMyBatis;

import java.util.Properties;
import java.util.concurrent.Callable;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checklist Bank multithreaded name usage solr indexer.
 * This class creates a pool of configurable <i>threads</i> that concurrently execute a number of jobs
 * each processing a configurable number of name usages (<i>batchSize</i>)
 * using a configurable number of concurrent lucene <i>writers</i>.
 * The indexer makes direct use of the mybatis layer and requires a checklist bank datasource to be configured.
 */
public class AvroExporter extends NameUsageBatchProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(AvroExporter.class);

  private String nameNode;
  private String targetHdfsDir;

  @Inject
  public AvroExporter(@Named(IndexingConfigKeys.THREADS) Integer threads,
                      @Named(IndexingConfigKeys.NAME_NODE) String nameNode,
                      @Named(IndexingConfigKeys.TARGET_HDFS_DIR) String targetHdfsDir,
                      @Named(IndexingConfigKeys.BATCH_SIZE) Integer batchSize,
                      @Named(IndexingConfigKeys.LOG_INTERVAL) Integer logInterval,
                      UsageService nameUsageService,
                      VernacularNameService vernacularNameService, DescriptionService descriptionService,
                      DistributionService distributionService, SpeciesProfileService speciesProfileService) {

    super(threads, batchSize, logInterval, nameUsageService, vernacularNameService, descriptionService, distributionService, speciesProfileService);
    this.nameNode = nameNode;
    this.targetHdfsDir = targetHdfsDir;
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
    Injector injector = Guice.createInjector(new AvroIndexingModule(props));
    // Gets the indexer instance
    AvroExporter nameUsageIndexer = injector.getInstance(AvroExporter.class);
    nameUsageIndexer.run();
    // This statement is used because the Guice container is not stopped inside the threadpool.
    LOG.info("Indexing done. Time to exit.");
    System.exit(0);
  }


  @Override
  protected Callable<Integer> newBatchJob(int startKey, int endKey, UsageService nameUsageService, VernacularNameServiceMyBatis vernacularNameService, DescriptionServiceMyBatis descriptionService, DistributionServiceMyBatis distributionService, SpeciesProfileServiceMyBatis speciesProfileService) {
    return new AvroExportJob(nameUsageService, startKey, endKey,
        vernacularNameService, descriptionService, distributionService, speciesProfileService, nameNode, targetHdfsDir);
  }

  @Override
  protected void init() throws Exception {
    // nothing to do
  }

  @Override
  protected void postprocess() throws Exception {
    // nothing to do
  }

}
