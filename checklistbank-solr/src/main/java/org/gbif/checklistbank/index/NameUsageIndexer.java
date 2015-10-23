package org.gbif.checklistbank.index;

import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.index.guice.EmbeddedSolrReference;
import org.gbif.checklistbank.index.guice.SolrIndexingModule;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.VernacularNameServiceMyBatis;
import org.gbif.utils.file.ResourcesUtil;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.core.CoreContainer;
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
  @Named("writers")
  private int numWriters = 1;

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


  // other injected instances
  private NameUsageDocConverter solrDocumentConverter;
  private EmbeddedSolrReference solrRef;
  private final File indexDir;

  //
  private List<Integer> allIds;
  private int jobCounter = 0;
  private EmbeddedSolrServer[] writers;


  private class CountReporter extends Thread {

    /**
     * Timer to measure the total time of execution
     */
    private StopWatch stopWatch = new StopWatch();
    private final long total;
    private final DecimalFormat twoDForm = new DecimalFormat("#.##");

    CountReporter(long total) {
      this.total = total;
    }

    @Override
    public void run() {
      stopWatch.start();
      LOG.info("Started reporting thread with expected {} total records.", total);
      LOG.info("Logging every {} seconds. Use logInterval property to change interval.", logInterval);
      boolean interrupted = false;
      while (!interrupted) {
        log();
        try {
          Thread.sleep(logInterval * 1000);
        } catch (InterruptedException e) {
          LOG.info("Reporter thread interrupted, exiting");
          interrupted = true;
        }
      }
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

  @Inject
  public NameUsageIndexer(EmbeddedSolrReference solr, @Named("threads") Integer threads,
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
    this.solrDocumentConverter = solrDocumentConverter;
    // final solr
    solrRef = solr;
    indexDir = new File(getSolrHome(), "parts");
    LOG.info("Creating solr indices in folder {}", indexDir.getAbsolutePath());
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
    NameUsageIndexer nameUsageIndexer = injector.getInstance(NameUsageIndexer.class);
    nameUsageIndexer.run();
    // This statement is used because the Guice container is not stopped inside the threadpool.
    System.exit(0);
  }

  private static Properties loadProperties(String propertiesFile) throws IOException {
    Properties tempProperties;
    try (Reader reader = Files.newReader(new File(propertiesFile), Charset.defaultCharset())) {
      tempProperties = new Properties();
      tempProperties.load(reader);
    }
    return tempProperties;
  }

  private void setupServers() {
    writers = new EmbeddedSolrServer[numWriters];
    if (numWriters == 1) {
      // use main server
      writers[0] = solrRef.getSolr();
    } else {
      // insert others
      LOG.debug("Setting up {} embedded solr servers ...", numWriters);
      for (int idx = 0; idx < numWriters; idx++) {
        writers[idx] = setupSolr(getWriterHome(idx));
      }
    }
  }

  private File getSolrHome() {
    return new File(solrRef.getSolr().getCoreContainer().getSolrHome());
  }

  private void mergeIndices() throws IOException, SolrServerException {
    if (numWriters == 1) {
      LOG.info("Optimizing single solr index ...");
      solrRef.getSolr().optimize();

    } else {
      File solrHome = getSolrHome();
      // shutdown solr before we can merge into its index
      solrRef.getSolr().getCoreContainer().shutdown();
      Path luceneDir = getLuceneDir(solrHome);
      LOG.debug("Opening main lucene index at {}", luceneDir);
      FSDirectory mainDir = FSDirectory.open(luceneDir);
      IndexWriterConfig cfg = new IndexWriterConfig(new StandardAnalyzer());
      IndexWriter fsWriter = new IndexWriter(mainDir, cfg);

      LOG.info("Start merging of {} solr indices", jobCounter);
      Directory[] parts = new Directory[jobCounter];
      for (int idx = 0; idx < jobCounter; idx++) {
        Path threadDir = getLuceneDir(getWriterHome(idx));
        LOG.info("Add lucene dir {} for merging", threadDir);
        parts[idx] = FSDirectory.open(threadDir);
      }
      fsWriter.addIndexes(parts);
      fsWriter.close();
      mainDir.close();
      LOG.info("Lucene dirs merged! Startup main solr again");

      //startup solr again, keeping it in the same singleton wrapper that is accessible to the other tests
      solrRef.setSolr(setupSolr(solrHome));
    }
  }

  private File getWriterHome(int thread) {
    return new File(indexDir, "slice" + thread);
  }

  private static Path getLuceneDir(File solrHome) {
    return Paths.get(solrHome.getPath(), "data/index");
  }

  /**
   * Setup an embedded solr only for with a given solr home.
   * Creates a checklistbank solr index schema, solr.xml and all other config files needed.
   *
   * @return the created server
   */
  private EmbeddedSolrServer setupSolr(File solrHome) {

    try {
      // copy solr resource files
      ResourcesUtil.copy(solrHome, "solr/", false, "solr.xml");
      // copy default configurations
      File conf = new File(solrHome, "conf");
      ResourcesUtil.copy(conf, "solr/default/", false, "synonyms.txt", "protwords.txt", "stopwords.txt");
      // copy specific configurations, overwriting above defaults
      ResourcesUtil.copy(conf, "solr/collection1/conf/", false, "schema.xml", "solrconfig.xml");

      // insert container
      CoreContainer coreContainer = CoreContainer.createAndLoad(solrHome.getAbsolutePath(), new File(solrHome, "solr.xml"));
      EmbeddedSolrServer solrServer = new EmbeddedSolrServer(coreContainer, "");

      LOG.info("Created embedded solr server with solr dir {}", solrHome.getAbsolutePath());

      // test solr
      SolrPingResponse solrPingResponse = solrServer.ping();
      LOG.info("Solr server configured at {}, ping response in {}", solrHome.getAbsolutePath(),
        solrPingResponse.getQTime());

      return solrServer;

    } catch (Exception e) {
      throw new IllegalStateException("Solr unavailable", e);
    }
  }

  @Override
  public int run() {
    int x = super.run();

    LOG.info("Time taken run and finish all jobs: {}", reporterThread.stopWatch.toString());
    // TODO: Fix deprecation issue
    reporterThread.stop();

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

    // round robin on configured solr servers?
    final SolrClient solrClient = writers[jobCounter % numWriters];

    return new NameUsageIndexingJob(solrClient, nameUsageService, startKey, endKey, solrDocumentConverter,
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

    // insert solr servers if multiple writers are configured
    setupServers();


    // start global reporter
    reporterThread = new CountReporter(allIds.size());
    reporterThread.start();
  }

  @Override
  protected void shutdownService(int tasksCount) {
    try {
      super.shutdownService(tasksCount);
      LOG.info("All jobs completed.");
      mergeIndices();
      LOG.info("Species Index rebuild completed!");
    } catch (Exception e) {
      LOG.error("Error shutingdown the index", e);
    }
  }
}
