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
package org.gbif.checklistbank.cli.importer;

import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.normalizer.Normalizer;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.cli.normalizer.NormalizerStats;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.nub.config.ClbNubConfiguration;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.nub.lookup.straight.IdLookupPassThru;
import org.gbif.utils.HttpUtil;
import org.gbif.utils.file.CompressionUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.postgresql.core.BaseConnection;

import com.beust.jcommander.internal.Maps;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

@Disabled("A manual test class")
/**
 * Test to manually index an external checklist and download, normalize and import it.
 * For importing it uses the test resource yaml config file where you can turn on search index if needed!
 */
public class ManualImport implements AutoCloseable {
  UUID datasetKey;
  private NormalizerConfiguration nCfg;
  private ImporterConfiguration iCfg;
  private final File repo;
  private final IdLookup lookup;

  public ManualImport(String repoDir, @Nullable ClbConfiguration nub) throws Exception {
    this.repo = new File(Preconditions.checkNotNull(repoDir));
    if (!repo.exists()) {
      repo.mkdirs();
    }
    if (nub == null) {
      System.out.println("Do not use matching");
      lookup = new IdLookupPassThru();
    } else {
      File lookupDB = new File(repo, "lookup.db");
      boolean existed = lookupDB.exists();
      IdLookupImpl db = IdLookupImpl.persistent(lookupDB);
      lookup = db;
      if (!existed) {
        System.out.println("Load lookup db for matching");
        db.load(ClbNubConfiguration.fromClbConfiguration(nub), false);
      } else {
        System.out.println("Use existing lookup db from "+lookupDB.getAbsolutePath());
      }
    }
  }

  public void index(String url, UUID datasetKey, boolean truncate) throws Exception {
    this.datasetKey = datasetKey;
    init(truncate);
    download(url);
    normalize();
    sync();
  }

  public void index(File dwca, UUID datasetKey, boolean truncate) throws Exception {
    this.datasetKey = datasetKey;
    init( truncate);
    copy(dwca);
    normalize();
    sync();
  }

  private void init(boolean truncate) throws IOException, SQLException {
    System.out.println("Init environment for dataset " + datasetKey);
    File dwca = new File(repo, "dwca");
    File neo = new File(repo, "neo");

    final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    nCfg = new NormalizerConfiguration();
    nCfg.neo = new NeoConfiguration();
    nCfg.neo.neoRepository = neo;
    nCfg.archiveRepository = dwca;

    iCfg = mapper.readValue(Resources.getResource("cfg-importer.yaml"), ImporterConfiguration.class);
    iCfg.neo = nCfg.neo;
    iCfg.deleteNeo = false;
    //iCfg.clb.serverName = "localhost";
    //iCfg.clb.databaseName = "clb";
    iCfg.clb.user = "postgres";
    iCfg.clb.password = "pogo";
    //iCfg.elasticsearch.host="http://devspeciessearch1-vh.gbif.org:9200/species/";

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

  private void copy(File fileOrFolder) throws IOException {
    File archiveFolder = nCfg.archiveDir(datasetKey);
    archiveFolder.mkdirs();
    if (fileOrFolder.isDirectory()) {
      System.out.println("Copy dwc archive folder to " + nCfg.archiveDir(datasetKey));
      FileUtils.copyDirectory(fileOrFolder, archiveFolder);

    } else {
      System.out.println("Copy dwc archive file to " + nCfg.archiveDir(datasetKey));
      FileUtils.copyFileToDirectory(fileOrFolder, archiveFolder);
    }
  }

  private void normalize() {
    MetricRegistry registry = new MetricRegistry();
    Normalizer norm = Normalizer.create(nCfg, datasetKey, registry, Maps.<String, UUID>newHashMap(), lookup);
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

  @Override
  public void close() throws Exception {
    lookup.close();
  }



  public static void main(String[] args) throws Exception {
    ClbConfiguration nub = new ClbConfiguration();
//    nub.serverName = "pg1.gbif.org";
//    nub.databaseName = "prod_checklistbank2";
    nub.user = "clb";
    nub.password = "";

    try (ManualImport imp = new ManualImport("/Users/markus/Downloads/repo", nub)) {

      imp.index("https://ipt.gbif.es/archive.do?r=especiessn_zoologiaugr",
          UUID.fromString("515c1dc0-b712-4fff-bc40-2b2ef2f9ecb9"), true);

//      imp.index("http://plazi.cs.umb.edu/GgServer/dwca/87A1ADC3C0C450976B05972ED1005EFC.zip",
//                UUID.fromString("0f66de86-d95f-47d1-af8d-b126ac38857a"), true);

//  imp.index(new File("/Users/markus/Downloads/ncbi.csv"),
//    UUID.fromString("61a5f178-b5fb-4484-b6d8-111111111111"), true);
    }


  }


}