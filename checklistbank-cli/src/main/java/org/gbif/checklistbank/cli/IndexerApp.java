package org.gbif.checklistbank.cli;


import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.importer.Importer;
import org.gbif.checklistbank.cli.importer.ImporterConfiguration;
import org.gbif.checklistbank.cli.normalizer.Normalizer;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.cli.normalizer.NormalizerStats;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.index.guice.Solr;
import org.gbif.checklistbank.nub.lookup.IdLookupPassThru;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.Mybatis;
import org.gbif.common.search.solr.SolrServerType;
import org.gbif.utils.HttpUtil;
import org.gbif.utils.file.CompressionUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.yammer.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Utility to manually index an external checklist and download, normalize and import it.
 * ATTENTION: this is a class mainly for debugging and configs are for simplicity all in the code !!!
 */
public class IndexerApp {
  UUID datasetKey;
  private NormalizerConfiguration nCfg;
  private ImporterConfiguration iCfg;
  private HikariDataSource hds;

  public void index(String repo, String url, UUID datasetKey) throws Exception {
    this.datasetKey = datasetKey;
    init(repo);
    download(url);
    normalize();
    sync();
  }

  private void init(String repo) throws IOException, SQLException {
    System.out.println("Init environment for dataset " + datasetKey);
    File tmp = new File(repo);
    File dwca = new File(tmp, "dwca");
    File neo = new File(tmp, "neo");

    nCfg = new NormalizerConfiguration();
    nCfg.neo = new NeoConfiguration();
    nCfg.neo.neoRepository = neo;
    nCfg.archiveRepository = dwca;

    iCfg = new ImporterConfiguration();
    iCfg.neo = nCfg.neo;
    iCfg.deleteNeo = false;
    iCfg.clb.serverName = "localhost";
    iCfg.clb.databaseName = "clb";
    iCfg.clb.user = "postgres";
    iCfg.clb.password = "pogo";
    iCfg.solr.serverType = SolrServerType.CLOUD;
    iCfg.solr.serverHome = "c1n1.gbif.org:2181,c1n2.gbif.org:2181,c1n3.gbif.org:2181/solrdev";
    iCfg.solr.collection = "dev_checklistbank";
    //iCfg.solr.serverType = SolrServerType.HTTP;
    //iCfg.solr.serverHome="http://apps2.gbif-dev.org:8082/checklistbank-solr";
  }

  private void download(String url) throws IOException {
    HttpUtil hutil = new HttpUtil(HttpUtil.newMultithreadedClient(2000, 5, 2));

    File dwca = File.createTempFile("clb", ".dwca");
    hutil.download(url, dwca);
    System.out.println("Downloaded raw archive to " + dwca.getAbsolutePath());
    CompressionUtil.decompressFile(nCfg.archiveDir(datasetKey), dwca);
    System.out.println("Decompressed archive to " + nCfg.archiveDir(datasetKey));
  }

  private void normalize() {
    MetricRegistry registry = new MetricRegistry("normalizer");
    Normalizer norm = Normalizer.create(nCfg, datasetKey, registry, Maps.<String, UUID>newHashMap(), new IdLookupPassThru());
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);
  }

  private void sync() throws Exception {
    // init mybatis layer and solr from cfg instance
    Injector inj = Guice.createInjector(iCfg.clb.createServiceModule(), new RealTimeModule(iCfg.solr));
    hds = (HikariDataSource) inj.getInstance(InternalChecklistBankServiceMyBatisModule.DATASOURCE_KEY);
    NameUsageService nameUsageService = inj.getInstance(NameUsageService.class);
    UsageService usageService = inj.getInstance(UsageService.class);
    DatasetImportService sqlService = inj.getInstance(Key.get(DatasetImportService.class, Mybatis.class));
    DatasetImportService solrService = inj.getInstance(Key.get(DatasetImportService.class, Solr.class));

    try {
      Importer importer = Importer.create(iCfg, datasetKey, nameUsageService, usageService, sqlService, solrService);
      importer.run();
    } finally {
      sqlService.close();
      solrService.close();
      hds.close();
    }
  }

  public static void main(String[] args) throws Exception {
    IndexerApp imp = new IndexerApp();

    //imp.index("/Users/markus/nub-repo", "", Constants.NUB_DATASET_KEY);

    imp.index("/Users/markus/Desktop/repo",
        "http://services.snsb.info/DTNtaxonlists/rest/v0.1/lists/DiversityTaxonNames_Fossils/1154/dwc",
        UUID.fromString("f096326f-8f98-4301-886b-d715e87e1d4e"));

//      imp.index("/Users/markus/Desktop/repo",
//                "http://bdj.pensoft.net/lib/ajax_srv/archive_download.php?archive_type=2&document_id=4878",
//                UUID.fromString("e18d65e8-3e8e-4cce-b309-256346f99de3"));

//    imp.index("/Users/markus/Desktop/repo",
//            "http://data.canadensys.net/ipt/archive.do?r=vascan",
//            UUID.fromString("3f8a1297-3259-4700-91fc-acc4170b27ce"));

//    imp.index("/Users/markus/Desktop/repo",
//            "http://www.catalogueoflife.org/DCA_Export/zip/archive-complete.zip",
//            UUID.fromString("7ddf754f-d193-4cc9-b351-99906754a03b"));

  }
}
