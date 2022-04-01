/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.cli;

import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.importer.ImporterConfiguration;
import org.gbif.checklistbank.cli.normalizer.Normalizer;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.cli.normalizer.NormalizerStats;
import org.gbif.nub.lookup.straight.IdLookupPassThru;
import org.gbif.utils.HttpUtil;
import org.gbif.utils.file.CompressionUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Maps;
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
    iCfg.clb.user = "postgres";
    iCfg.clb.password = "pogo";
    iCfg.elasticsearch.hosts = "http://c3n1.gbif.org:9200/,http://c3n2.gbif.org:9200/,http://c3n3.gbif.org:9200/";
    iCfg.elasticsearch.index = "species";
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
    Normalizer norm = Normalizer.create(nCfg, datasetKey, registry, Maps.<String, UUID>newHashMap(), new IdLookupPassThru());
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);
  }

  private void sync() throws Exception {
    // init mybatis layer and Elasticsearch from cfg instance
//    Injector inj = Guice.createInjector(ChecklistBankServiceMyBatisModule.create(iCfg.clb), new RealTimeModule(iCfg.elasticsearch));
//    hds = (HikariDataSource) inj.getInstance(InternalChecklistBankServiceMyBatisModule.DATASOURCE_KEY);
//    NameUsageService nameUsageService = inj.getInstance(NameUsageService.class);
//    UsageService usageService = inj.getInstance(UsageService.class);
//    DatasetImportService sqlService = inj.getInstance(Key.get(DatasetImportService.class, Mybatis.class));
//    DatasetImportService searchIndexService = inj.getInstance(Key.get(DatasetImportService.class, Solr.class));
//
//    try {
//      Importer importer = Importer.create(iCfg, datasetKey, nameUsageService, usageService, sqlService, searchIndexService);
//      importer.run();
//    } finally {
//      sqlService.close();
//      searchIndexService.close();
//      hds.close();
//    }
  }

  public static void main(String[] args) throws Exception {
    IndexerApp imp = new IndexerApp();

    //imp.index("/Users/markus/nub-repo", "", Constants.NUB_DATASET_KEY);

    imp.index("/Users/markus/Desktop/repo",
        "http://services.snsb.info/DTNtaxonlists/rest/v0.1/lists/DiversityTaxonNames_Fossils/1154/dwc",
        UUID.fromString("f096326f-8f98-4301-886b-d715e87e1d4e"));

  }
}
