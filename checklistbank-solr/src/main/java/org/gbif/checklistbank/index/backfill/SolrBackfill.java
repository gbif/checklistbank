package org.gbif.checklistbank.index.backfill;

import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.index.NameUsageDocConverter;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
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
public class SolrBackfill extends NameUsageBatchProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(SolrBackfill.class);

  private final int numWriters;

  // other injected instances
  private NameUsageDocConverter solrDocumentConverter;
  private final File indexDir;
  private EmbeddedSolrReference solrRef;
  private EmbeddedSolrServer[] writers;


  @Inject
  public SolrBackfill(EmbeddedSolrReference solr,
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
    SolrBackfill nameUsageIndexer = injector.getInstance(SolrBackfill.class);
    nameUsageIndexer.run();
    // This statement is used because the Guice container is not stopped inside the threadpool.
    System.exit(0);
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
      ResourcesUtil.copy(conf, "solr/checklistbank/conf/", false, "schema.xml", "solrconfig.xml");

      // insert container
      CoreContainer coreContainer = new CoreContainer(solrHome.getAbsolutePath());
      coreContainer.load();

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
  protected Callable<Integer> newBatchJob(int startKey, int endKey, UsageService nameUsageService, VernacularNameServiceMyBatis vernacularNameService, DescriptionServiceMyBatis descriptionService, DistributionServiceMyBatis distributionService, SpeciesProfileServiceMyBatis speciesProfileService) {
    // round robin on configured solr servers?
    final SolrClient solrClient = writers[jobCounter % numWriters];
    return new NameUsageIndexingJob(solrClient, nameUsageService, startKey, endKey, solrDocumentConverter,
        vernacularNameService, descriptionService, distributionService, speciesProfileService);
  }

  @Override
  protected void init() throws Exception {
    // insert solr servers if multiple writers are configured
    setupServers();
  }

  @Override
  protected void postprocess() throws Exception {
    mergeIndices();
  }

}
