package org.gbif.checklistbank.index;

import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.index.guice.IndexingModule;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.VernacularNameServiceMyBatis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checklist Bank multithreaded name usage solr indexer.
 * This class creates a pool of configurable <i>threads</i> that concurrently execute a number of jobs
 * each processing a configurable number of name usages (<i>batchSize</i>)
 * using a configurable number of concurrent lucene <i>writers</i>.
 * The indexer makes direct use of the mybatis layer and requires a checklist bank datasource to be configured.
 */
public class NameUsageIndexer extends ThreadPoolRunner<Integer> {

  protected static AtomicLong counter = new AtomicLong(0L);
  private static final Logger LOG = LoggerFactory.getLogger(NameUsageIndexer.class);

  @Inject(optional = true)
  @Named("batchSize")
  private int batchSize = 10000;

  /**
   * Log interval in seconds. Use property logInterval to set it, defaults to one minute.
   */
  @Inject(optional = true)
  @Named("logInterval")
  private Integer logInterval = 60;
  private CountReporter reporterThread;

  // mybatis converted services exposing internal methods not avaiable in the service interface
  // (would also have to be in clients then)
  private final UsageService nameUsageService;
  private final VernacularNameServiceMyBatis vernacularNameService;
  private final DescriptionServiceMyBatis descriptionService;
  private final DistributionServiceMyBatis distributionService;
  private final SpeciesProfileServiceMyBatis speciesProfileService;


  //
  private List<Integer> allIds;
  private int jobCounter = 0;


  private class CountReporter extends Thread {

    /**
     * Timer to measure the total time of execution
     */
    private StopWatch stopWatch = new StopWatch();
    private final long total;
    private final DecimalFormat twoDForm = new DecimalFormat("#.##");
    private boolean stop;

    CountReporter(long total) {
      this.total = total;
    }

    @Override
    public void run() {
      stopWatch.start();
      LOG.info("Started reporting thread with expected {} total records.", total);
      LOG.info("Logging every {} seconds. Use logInterval property to change interval.", logInterval);
      stop = false;
      while (!stop) {
        log();
        try {
          Thread.sleep(logInterval * 1000);
        } catch (InterruptedException e) {
          LOG.info("Reporter thread interrupted, exiting");
          stop = true;
        }
      }
      LOG.info("Reporter thread stopping");
    }

    /**
     * Shuts down the reporter thread.
     */
    public void shutdown() {
      stop = true;
    }

    /**
     * Log total progress every minute.
     */
    private void log() {
      long cnt = counter.get();
      double percCompleted = (double) cnt / (double) total;
      double percRemaining = 1d - percCompleted;
      long timeRemaining = (long) (stopWatch.getTime() * (percRemaining / percCompleted));
      LOG.info("{} documents ({}%) added in {}", cnt, twoDForm.format(percCompleted * 100), stopWatch.toString());
      LOG.info("Expected remaining time to finish {}", DurationFormatUtils.formatDurationHMS(timeRemaining));
    }

  }

  @Inject
  public NameUsageIndexer(@Named("threads") Integer threads,
    UsageService nameUsageService, NameUsageDocConverter solrDocumentConverter,
    VernacularNameService vernacularNameService, DescriptionService descriptionService,
    DistributionService distributionService, SpeciesProfileService speciesProfileService) {

    super(threads);
    this.vernacularNameService = (VernacularNameServiceMyBatis) vernacularNameService;
    this.descriptionService = (DescriptionServiceMyBatis) descriptionService;
    this.distributionService = (DistributionServiceMyBatis) distributionService;
    this.speciesProfileService = (SpeciesProfileServiceMyBatis) speciesProfileService;
    // services
    this.nameUsageService = nameUsageService;
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
    Injector injector = Guice.createInjector(new IndexingModule(props));
    // Gets the indexer instance
    NameUsageIndexer nameUsageIndexer = injector.getInstance(NameUsageIndexer.class);
    nameUsageIndexer.run();
    // This statement is used because the Guice container is not stopped inside the threadpool.
    LOG.info("Indexing done. Time to exit.");
    System.exit(0);
  }

  private static Properties loadProperties(String propertiesFile) throws IOException {
    InputStream inputStream = null;
    Properties tempProperties;
    try {
      inputStream = Files.newInputStreamSupplier(new File(propertiesFile)).getInput();
      tempProperties = new Properties();
      tempProperties.load(inputStream);
    } finally {
      Closeables.closeQuietly(inputStream);
    }
    return tempProperties;
  }

  @Override
  public int run() {
    int x = super.run();

    LOG.info("Time taken run and finish all jobs: {}", reporterThread.stopWatch.toString());
    reporterThread.shutdown();

    return x;
  }

  /**
   * Creates a list of NameUsageIndexingJob by loading all usage ids and splitting up the jobs between those ids.
   *
   * @return a {@link List} of {@link NameUsageIndexingJob}.
   */
  @Override
  protected Callable<Integer> newJob() {
    if (allIds == null) {
      init();
    }

    // any new job to be created?
    if (allIds.size() <= jobCounter * batchSize) {
      LOG.info("No more jobs to insert. Created {} jobs in total each processing {} records.", jobCounter, batchSize);
      return null;
    }

    // produce new job with a new slice
    final int startKey = allIds.get(jobCounter * batchSize);
    int endIdx = (jobCounter + 1) * batchSize - 1;
    final int endKey = endIdx > allIds.size() ? allIds.get(allIds.size() - 1) : allIds.get(endIdx);
    jobCounter++;


    return new NameUsageIndexingJob(nameUsageService, startKey, endKey,
      vernacularNameService, descriptionService, distributionService, speciesProfileService);
  }

  private void init() {
    StopWatch stopWatch = new StopWatch();

    LOG.debug("Start retrieving all usage ids ...");
    stopWatch.start();
    allIds = nameUsageService.listAll();
    LOG.info("Retrieved all {} usage ids in {}", allIds.size(), stopWatch.toString());
    stopWatch.reset();
    stopWatch.start();
    Collections.sort(allIds);
    LOG.info("Sorted all {} usage ids in {}", allIds.size(), stopWatch.toString());
    LOG.info("{} full jobs each processing {} records to be created.", allIds.size() / batchSize, batchSize);

    // start global reporter
    reporterThread = new CountReporter(allIds.size());
    reporterThread.start();
  }

  @Override
  protected void shutdownService(int tasksCount) {
    try {
      super.shutdownService(tasksCount);
      LOG.info("All jobs completed.");
    } catch (Exception e) {
      LOG.error("Error shutingdown the index", e);
    }
  }
}
