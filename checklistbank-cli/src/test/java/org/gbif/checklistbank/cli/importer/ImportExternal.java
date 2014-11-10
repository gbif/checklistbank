package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.normalizer.Normalizer;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.cli.normalizer.NormalizerService;
import org.gbif.utils.HttpUtil;
import org.gbif.utils.file.CompressionUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import com.beust.jcommander.internal.Maps;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.jvm.MemoryUsageGaugeSet;
import org.junit.Ignore;

@Ignore("A manual test class")
/**
 * Test to manually index an external checklist and download, normalize and import it.
 * For importing it uses the test resource yaml config file where you can turn on solr if needed!
 */
public class ImportExternal {
  UUID datasetKey;
  private NormalizerConfiguration nCfg;
  private ImporterConfiguration iCfg;

  public void index(String repo, String url, UUID datasetKey) throws IOException, SQLException {
    this.datasetKey = datasetKey;
    init(repo);
//    download(url);
//    normalize();
    sync();
  }

  private void init(String repo) throws IOException {
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
    iCfg.deleteNeo=false;
    iCfg.clb.serverName="localhost";
    iCfg.clb.databaseName="clb";
    iCfg.clb.user ="postgres";
    iCfg.clb.password="%BBJu2MgstXJ";
    iCfg.clb.password="pogo";
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
    MemoryUsageGaugeSet mgs = new MemoryUsageGaugeSet();
    registry.registerAll(mgs);

    registry.meter(NormalizerService.INSERT_METER);
    registry.meter(NormalizerService.RELATION_METER);
    registry.meter(NormalizerService.METRICS_METER);
    registry.meter(NormalizerService.DENORMED_METER);

    Normalizer norm = new Normalizer(nCfg, datasetKey, registry, Maps.<String, UUID>newHashMap(), nCfg.matching.createMatchingService());
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);
  }

  private void sync() throws SQLException {
    ImporterIT iit = new ImporterIT();
    Importer importer = iit.build(iCfg, datasetKey);
    importer.run();
  }

  public static void main(String[] args) throws Exception{
    ImportExternal imp = new ImportExternal();
    //imp.index("http://ipt.speciesfile.org:8080/archive.do?r=coleorrhyncha", UUID.fromString(""));
    imp.index("/Users/markus/Desktop/repo", "http://ipt.speciesfile.org:8080/archive.do?r=blattodea", UUID.fromString("7ddf754f-d193-4cc9-b351-99906754a03b"));

  }
}
