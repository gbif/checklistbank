package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.normalizer.Normalizer;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.cli.normalizer.NormalizerTest;
import org.gbif.utils.HttpUtil;
import org.gbif.utils.file.CompressionUtil;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
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

  public void index(String url, UUID datasetKey) throws IOException {
    this.datasetKey = datasetKey;
    init();
    download(url);
    normalize();
    sync();
  }

  private void init() throws IOException {
    System.out.println("Init environment for dataset " + datasetKey);
    File tmp = new File("/tmp/gbif-storage");
    File dwca = new File(tmp, "dwca");
    File neo = new File(tmp, "neo");

    nCfg = new NormalizerConfiguration();
    nCfg.neo = new NeoConfiguration();
    nCfg.neo.neoRepository = neo;
    nCfg.archiveRepository = dwca;

    final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    iCfg = mapper.readValue(Resources.getResource("cfg-importer.yaml"), ImporterConfiguration.class);
    iCfg.neo = nCfg.neo;
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
    Normalizer norm = NormalizerTest.buildNormalizer(nCfg, datasetKey);
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);
  }

  private void sync() {
    Importer importer = ImporterIT.build(iCfg, datasetKey);
    importer.run();
  }

  public static void main(String[] args) throws Exception{
    ImportExternal imp = new ImportExternal();
    imp.index("http://ipt.speciesfile.org:8080/archive.do?r=coleorrhyncha", UUID.fromString(""));
  }
}
