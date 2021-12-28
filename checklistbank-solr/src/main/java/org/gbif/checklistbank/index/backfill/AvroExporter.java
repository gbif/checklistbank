package org.gbif.checklistbank.index.backfill;

import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.index.guice.SpringSolrConfig;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.service.*;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Checklist Bank multithreaded name usage solr indexer. This class creates a pool of configurable
 * <i>threads</i> that concurrently execute a number of jobs each processing a configurable number
 * of name usages (<i>batchSize</i>) using a configurable number of concurrent lucene
 * <i>writers</i>. The indexer makes direct use of the mybatis layer and requires a checklist bank
 * datasource to be configured.
 */
@SpringBootApplication
@Import({SpringSolrConfig.class, SpringServiceConfig.class})
public class AvroExporter extends NameUsageBatchProcessor implements CommandLineRunner {

  private static final Logger LOG = LoggerFactory.getLogger(AvroExporter.class);

  private String nameNode;
  private String targetHdfsDir;

  @Autowired
  public AvroExporter(
      @Value("${" + IndexingConfigKeys.THREADS + "}") Integer threads,
      @Value("${" + IndexingConfigKeys.NAME_NODE + "}") String nameNode,
      @Value("${" + IndexingConfigKeys.TARGET_HDFS_DIR + "}") String targetHdfsDir,
      @Value("${" + IndexingConfigKeys.BATCH_SIZE + "}") Integer batchSize,
      @Value("${" + IndexingConfigKeys.LOG_INTERVAL + "}") Integer logInterval,
      UsageService nameUsageService,
      VernacularNameService vernacularNameService,
      DescriptionService descriptionService,
      DistributionService distributionService,
      SpeciesProfileService speciesProfileService) {

    super(
        threads,
        batchSize,
        logInterval,
        nameUsageService,
        vernacularNameService,
        descriptionService,
        distributionService,
        speciesProfileService);
    this.nameNode = nameNode;
    this.targetHdfsDir = targetHdfsDir;
  }

  /** Entry point for execution. Commandline arguments are: 0: required path to property file */
  public static void main(String[] args) throws Exception {
    SpringApplication app = new SpringApplication(AvroExporter.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    app.setBannerMode(Banner.Mode.OFF);
    app.run(args);
  }

  @Override
  public void run(String... args) throws Exception {
    if (args.length == 0) {
      throw new IllegalArgumentException("Path to property file required");
    }
    run();
    // This statement is used because the Guice container is not stopped inside the threadpool.
    LOG.info("Indexing done. Time to exit.");
    System.exit(0);
  }

  @Override
  protected Callable<Integer> newBatchJob(
      int startKey,
      int endKey,
      UsageService nameUsageService,
      VernacularNameServiceMyBatis vernacularNameService,
      DescriptionServiceMyBatis descriptionService,
      DistributionServiceMyBatis distributionService,
      SpeciesProfileServiceMyBatis speciesProfileService) {
    return new AvroExportJob(
        nameUsageService,
        startKey,
        endKey,
        vernacularNameService,
        descriptionService,
        distributionService,
        speciesProfileService,
        nameNode,
        targetHdfsDir);
  }
}
