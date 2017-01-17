package org.gbif.checklistbank.cli.importer;


import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.normalizer.Normalizer;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.cli.normalizer.NormalizerStats;
import org.gbif.nub.lookup.fuzzy.NubMatchingPassThru;
import org.gbif.utils.HttpUtil;
import org.gbif.utils.file.CompressionUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import com.beust.jcommander.internal.Maps;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.junit.Ignore;
import org.postgresql.core.BaseConnection;

@Ignore("A manual test class")
/**
 * Test to manually index an external checklist and download, normalize and import it.
 * For importing it uses the test resource yaml config file where you can turn on solr if needed!
 */
public class ManualImport {
  UUID datasetKey;
  private NormalizerConfiguration nCfg;
  private ImporterConfiguration iCfg;

  public void index(String repo, String url, UUID datasetKey) throws Exception {
    this.datasetKey = datasetKey;
    init(repo, false);
    download(url);
    normalize();
    sync();
  }

  private void init(String repo, boolean truncate) throws IOException, SQLException {
    System.out.println("Init environment for dataset " + datasetKey);
    File tmp = new File(repo);
    File dwca = new File(tmp, "dwca");
    File neo = new File(tmp, "neo");

    final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    nCfg = new NormalizerConfiguration();
    nCfg.neo = new NeoConfiguration();
    nCfg.neo.neoRepository = neo;
    nCfg.archiveRepository = dwca;

    iCfg = mapper.readValue(Resources.getResource("cfg-importer.yaml"), ImporterConfiguration.class);
    iCfg.neo = nCfg.neo;
    iCfg.deleteNeo = false;
    iCfg.clb.serverName = "localhost";
    iCfg.clb.databaseName = "clb";
    iCfg.clb.user = "postgres";
    iCfg.clb.password = "pogo";
    //iCfg.solr.serverType = SolrServerType.HTTP;
    //iCfg.solr.serverHome="http://apps2.gbif-dev.org:8082/checklistbank-solr";

    // truncate tables?
    if (truncate) {
      try (BaseConnection c = (BaseConnection) iCfg.clb.connect()){
        try (Statement st = c.createStatement()) {
          for (String table : Lists.newArrayList("name_usage_metrics", "raw_usage", "name_usage", "citation", "name")) {
            st.execute("TRUNCATE "+table+" CASCADE");
          }
        }
      }
      System.out.println("Truncated clb tables");
    }
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
    MetricRegistry registry = new MetricRegistry();
    Normalizer norm = Normalizer.create(nCfg, datasetKey, registry, Maps.<String, UUID>newHashMap(), new NubMatchingPassThru());
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);
  }

  private void sync() throws Exception {
    ImporterIT iit = new ImporterIT();
    Importer importer = iit.build(iCfg, datasetKey);
    importer.run();
    iit.close();
  }

  public static void main(String[] args) throws Exception {
    ManualImport imp = new ManualImport();

//    imp.index("/Users/markus/nub-repo", "", Constants.NUB_DATASET_KEY);

//      imp.index("/Users/markus/Desktop/repo",
//                "http://plazi.cs.umb.edu/GgServer/dwca/87A1ADC3C0C450976B05972ED1005EFC.zip",
//                UUID.fromString("0f66de86-d95f-47d1-af8d-b126ac38857a"));

    imp.index("/Users/markus/Desktop/repo",
            "http://data.canadensys.net/ipt/archive.do?r=vascan",
            UUID.fromString("3f8a1297-3259-4700-91fc-acc4170b27ce"));

//    imp.index("/Users/markus/Desktop/repo",
//            "http://www.catalogueoflife.org/DCA_Export/zip/archive-complete.zip",
//            UUID.fromString("7ddf754f-d193-4cc9-b351-99906754a03b"));

  }
}