package org.gbif.checklistbank.index.backfill;

import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.VernacularNameServiceMyBatis;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.io.Files;
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
public abstract class NameUsageBatchProcessor extends ThreadPoolRunner<Integer> {

  // document counter
  protected static AtomicLong counter = new AtomicLong(0L);
  private static final Logger LOG = LoggerFactory.getLogger(NameUsageBatchProcessor.class);

  protected final int batchSize;

  /**
   * Log interval in seconds. Use property logInterval to set it, defaults to one minute.
   */
  protected final Integer logInterval;
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
  protected int jobCounter = 0;

  private class CountReporter extends Thread {

    /**
     * Timer to measure the total time of execution
     */
    private StopWatch stopWatch = new StopWatch();
    private final long total;
    private final DecimalFormat twoDForm = new DecimalFormat("#.##");
    private boolean interrupted = false;

    CountReporter(long total) {
      this.total = total;
    }

    @Override
    public void run() {
      stopWatch.start();
      LOG.info("Started reporting thread with expected {} total records.", total);
      LOG.info("Logging every {} seconds. Use logInterval property to change interval.", logInterval);
      while (!interrupted) {
        log();
        try {
          Thread.sleep(logInterval * 1000);
        } catch (InterruptedException e) {
          LOG.info("Reporter thread interrupted, exiting");
          interrupted = true;
        }
      }
      LOG.info("Reporter thread stopped");
    }

    public void shutdown() {
      interrupted = true;
    }

    /**
     * Log total progress every minute.
     */
    private void log() {
      long cnt = counter.get();
      double percCompleted = (double) cnt / (double) total;
      double percRemaining = 1d - percCompleted;
      long timeRemaining = (long) (stopWatch.getTime() * (percRemaining / percCompleted));
      LOG.info("{} documents ({}%) added in {}",
        new Object[] {cnt, twoDForm.format(percCompleted * 100), stopWatch.toString()});
      LOG.info("Expected remaining time to finish {}", DurationFormatUtils.formatDurationHMS(timeRemaining));
    }

  }

  public NameUsageBatchProcessor(Integer threads, int batchSize, Integer logInterval,
                                 UsageService nameUsageService,
                                 VernacularNameService vernacularNameService, DescriptionService descriptionService,
                                 DistributionService distributionService, SpeciesProfileService speciesProfileService) {

    super(threads);
    this.logInterval = logInterval;
    this.batchSize = batchSize;
    // services
    this.nameUsageService = nameUsageService;
    this.vernacularNameService = (VernacularNameServiceMyBatis) vernacularNameService;
    this.descriptionService = (DescriptionServiceMyBatis) descriptionService;
    this.distributionService = (DistributionServiceMyBatis) distributionService;
    this.speciesProfileService = (SpeciesProfileServiceMyBatis) speciesProfileService;
  }

  public static Properties loadProperties(String propertiesFile) throws IOException {
    Properties tempProperties;
    try (Reader reader = Files.newReader(new File(propertiesFile), Charset.defaultCharset())) {
      tempProperties = new Properties();
      tempProperties.load(reader);
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
      initKeys();
      try {
        init();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
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

    return newBatchJob(startKey, endKey, nameUsageService, vernacularNameService, descriptionService, distributionService, speciesProfileService);
  }

  protected abstract Callable<Integer> newBatchJob(int startKey, int endKey, UsageService nameUsageService, VernacularNameServiceMyBatis vernacularNameService, DescriptionServiceMyBatis descriptionService, DistributionServiceMyBatis distributionService, SpeciesProfileServiceMyBatis speciesProfileService);

  protected abstract void init() throws Exception;

  protected abstract void postprocess() throws Exception;

  private void initKeys() {
    StopWatch stopWatch = new StopWatch();

    LOG.debug("Start retrieving all usage ids ...");
    stopWatch.start();
    allIds = nameUsageService.listAll();
    //allIds = Lists.newArrayList(ContiguousSet.create(Range.closed(0, 13), DiscreteDomain.integers()).asList());

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
      postprocess();
      LOG.info("Indexing completed!");
    } catch (Exception e) {
      LOG.error("Error shutingdown the indexer", e);
    }
  }
}
