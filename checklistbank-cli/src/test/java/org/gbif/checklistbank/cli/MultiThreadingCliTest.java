package org.gbif.checklistbank.cli;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.io.FileUtils;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.cli.importer.Importer;
import org.gbif.checklistbank.cli.importer.ImporterConfiguration;
import org.gbif.checklistbank.cli.normalizer.Normalizer;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.index.guice.Solr;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.Mybatis;
import org.gbif.checklistbank.utils.ResourcesMonitor;
import org.gbif.checklistbank.utils.RunnableAdapter;
import org.gbif.common.search.solr.SolrServerType;
import org.gbif.utils.file.CompressionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Ignore("manual long running test to discover why we see too many open files in heavy importer cli use")
public class MultiThreadingCliTest {
  private static final ObjectMapper CFG_MAPPER = new ObjectMapper(new YAMLFactory());
  private final int threads = 5;

  private MetricRegistry registry;
  private NormalizerConfiguration cfgN;
  private ImporterConfiguration cfgI;
  private File zip;
  private ResourcesMonitor monitor;
  private NameUsageService usageService;
  private HikariDataSource hds;
  DatasetImportService sqlService;
  DatasetImportService solrService;

  @Before
  public void init() throws Exception {
    registry = new MetricRegistry();

    cfgN = new NormalizerConfiguration();
    cfgN.neo.neoRepository = Files.createTempDirectory("neotest").toFile();
    cfgN.archiveRepository = Files.createTempDirectory("neotestdwca").toFile();

    cfgI = CFG_MAPPER.readValue(Resources.getResource("cfg-importer.yaml"), ImporterConfiguration.class);
    cfgI.neo = cfgN.neo;

    System.out.println("Using postgres instance" + cfgI.clb.serverName + " " + cfgI.clb.databaseName);

    zip = new File(getClass().getResource("/plazi.zip").getFile());
    zip = new File("/Users/markus/code/checklistbank/checklistbank-cli/src/test/resources/plazi.zip");

    Timer timer = new Timer();
    monitor = new ResourcesMonitor();
    timer.schedule(monitor, 1000);
  }

  @After
  public void cleanup() {
    FileUtils.deleteQuietly(cfgN.neo.neoRepository);
    FileUtils.deleteQuietly(cfgN.archiveRepository);
    if (hds != null) {
      hds.close();
    }
  }

  @Test
  public void manyNormalizersInParallel() throws Exception {
    final int tasks = 500;

    ExecutorCompletionService<Object> ecs = new ExecutorCompletionService(Executors.newFixedThreadPool(threads));
    List<Future<Object>> futures = Lists.newArrayList();
    for (int i = 0; i < tasks; i++) {
      UUID dk = UUID.randomUUID();

      // copy dwca
      File dwca = cfgN.archiveDir(dk);
      CompressionUtil.decompressFile(dwca, this.zip);

      Normalizer normalizer = Normalizer.create(cfgN, registry, dk);
      System.out.println("Submit normalizer " + i);
      futures.add(ecs.submit(Executors.callable(normalizer)));
    }

    int idx = 1;
    for (Future<Object> f : futures) {
      f.get();
      System.out.println("Finished normalizer " + idx++);
      monitor.run();
    }
    System.out.println("Finished all jobs");
    monitor.run();
    for (Thread t : Thread.getAllStackTraces().keySet()) {
      System.out.println(t.getState() + " " + t.getName());
    }
  }

  @Test
  public void manyImporterInParallel() throws Exception {
    final int tasks = 1000;

    PrintStream log = System.out;

    // init mybatis layer and solr from cfgN instance
    cfgI.solr.setServerHome("http://apps2.gbif-dev.org:8082/checklistbank-solr");
    cfgI.solr.setServerType(SolrServerType.HTTP);
    Injector inj = Guice.createInjector(ChecklistBankServiceMyBatisModule.create(cfgI.clb), new RealTimeModule(cfgI.solr));
    usageService = inj.getInstance(NameUsageService.class);
    sqlService = inj.getInstance(Key.get(DatasetImportService.class, Mybatis.class));
    solrService = inj.getInstance(Key.get(DatasetImportService.class, Solr.class));

    hds = (HikariDataSource) inj.getInstance(InternalChecklistBankServiceMyBatisModule.DATASOURCE_KEY);
    // truncate tables
    log.println("Truncate existing data");
    Connection cn = hds.getConnection();
    java.sql.Statement st = cn.createStatement();
    st.execute("truncate name_usage cascade");
    st.execute("truncate name cascade");
    st.execute("truncate citation cascade");
    st.close();
    cn.close();

    ExecutorCompletionService<UUID> ecs = new ExecutorCompletionService(Executors.<UUID>newFixedThreadPool(threads));
    LinkedList<Future<UUID>> futures = Lists.newLinkedList();

    log.println("Start creating normalization tasks");
    LinkedList<Normalizer> normalizers = Lists.newLinkedList();
    for (int i = 0; i < tasks; i++) {
      UUID dk = UUID.randomUUID();
      // copy dwca
      File dwca = cfgN.archiveDir(dk);
      CompressionUtil.decompressFile(dwca, this.zip);
      normalizers.add(Normalizer.create(cfgN, registry, dk));
    }

    log.println("Submitted tasks ...");


    for (int x = 0; x < threads; x++) {
      Normalizer n = normalizers.removeFirst();
      futures.add(ecs.submit(Executors.callable(n, n.getDatasetKey())));
    }
    while (!futures.isEmpty()) {
      Future<UUID> f = futures.pop();
      UUID dk = f.get();
      if (dk != null) {
        // this was a normalizer, submit its importer
        log.println("Finished normalizer " + dk + " with open files: " + monitor.getOpenFileDescriptorCount());
        futures.add(ecs.submit(new RunnableAdapter(buildImporter(dk), null)));
      } else {
        log.println("Finished importer with open files: " + monitor.getOpenFileDescriptorCount());
        // add a new normalizer if we still have some
        if (!normalizers.isEmpty()) {
          Normalizer n = normalizers.removeFirst();
          futures.add(ecs.submit(Executors.callable(n, n.getDatasetKey())));
        }
      }
    }
    log.println("Finished all tasks. Done");
    monitor.run();
  }

  public Importer buildImporter(UUID datasetKey) throws SQLException {
    return Importer.create(cfgI, datasetKey, usageService, null, sqlService, solrService);
  }
}
