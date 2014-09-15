package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.vocabulary.Origin;
import org.gbif.checklistbank.cli.normalizer.NeoTest;
import org.gbif.checklistbank.cli.normalizer.Normalizer;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.cli.normalizer.NormalizerIT;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.apache.ibatis.io.Resources;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Importer tests, using the normalizer test dwcas to first produce a neo4j db and then import that into postgres.
 * //TODO: once tests are stable consider to keep neo db as direct test resource?
 */
public class ImporterIT extends NeoTest {

  private static final ObjectMapper CFG_MAPPER = new ObjectMapper(new YAMLFactory());
  private NormalizerConfiguration nCfg;
  private ImporterConfiguration iCfg;
  private NameUsageService usageService;

  @Before
  public void initDwcaRepo() throws Exception {
    nCfg = new NormalizerConfiguration();
    nCfg.neo = super.cfg;

    URL dwcasUrl = getClass().getResource("/dwcas");
    Path p = Paths.get(dwcasUrl.toURI());
    nCfg.archiveRepository = p.toFile();

    iCfg = CFG_MAPPER.readValue(Resources.getResourceAsStream("cfg-importer.yaml"), ImporterConfiguration.class);
    iCfg.neo = nCfg.neo;
    Injector inj = Guice.createInjector(iCfg.clb.createServiceModule());
    usageService = inj.getInstance(NameUsageService.class);
    // truncate tables
    truncate(inj.getInstance(
      Key.get(DataSource.class, Names.named(InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME))
    ));
  }

  private void truncate(DataSource ds) throws SQLException {
    try (Connection con = ds.getConnection()){
      try (Statement st = con.createStatement()){
        st.execute("TRUNCATE citation CASCADE");
        st.execute("TRUNCATE name CASCADE");
      }
    }
  }

  @Test
  public void testIdList() {
    final UUID datasetKey = NormalizerIT.datasetKey(1);

    Importer importer = Importer.build(iCfg, datasetKey);
    // insert neo db
    Normalizer norm = Normalizer.build(nCfg, datasetKey, null);
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);

    // import
    importer.run();
  }

  /**
   * Testing CLIMBER dataset from ZooKeys:
   * http://www.gbif.org/dataset/e2bcea8c-dfea-475e-a4ae-af282b4ea1c5
   *
   * Especially the behavior of acceptedNameUsage (canonical form withut authorship)
   * pointing to itself (scientificName WITH authorship) indicating this is NOT a synonym.
   */
  @Test
  public void testVerbatimAccepted() throws Exception {
    final UUID datasetKey = NormalizerIT.datasetKey(14);

    Importer importer = Importer.build(iCfg, datasetKey);
    // insert neo db
    Normalizer norm = Normalizer.build(nCfg, datasetKey, null);
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);

    // import
    importer.run();

    // test db, all usages must be accepted and there is one root!
    PagingResponse<NameUsage> resp = usageService.list(null, datasetKey, null, new PagingRequest(0, 100));
    assertEquals(16, resp.getResults().size());
    for (NameUsage u : resp.getResults()) {
      assertFalse(u.isSynonym());
      assertNull(u.getAcceptedKey());
      assertNull(u.getAccepted());
      assertNull(u.getBasionymKey());
      assertNull(u.getBasionym());
      assertNotNull(u.getOrigin());
      if (u.getScientificName().equals("Animalia")) {
        assertNull(u.getParentKey());
        assertNull(u.getParent());
      } else {
        assertNotNull(u.getParentKey());
        assertNotNull(u.getParent());
      }
    }
  }


  /**
   * Reimport the same dataset and make sure ids stay the same.
   */
  @Test
  public void testStableIds() throws Exception {
    final UUID datasetKey = NormalizerIT.datasetKey(14);

    Importer importer = Importer.build(iCfg, datasetKey);
    // insert neo db
    Normalizer norm = Normalizer.build(nCfg, datasetKey, null);
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);

    // 1st import
    importer.run();
    // remember ids
    Map<Integer, String> ids = Maps.newHashMap();
    int sourceCounter = 0;
    PagingResponse<NameUsage> resp = usageService.list(null, datasetKey, null, new PagingRequest(0, 100));
    assertEquals(16, resp.getResults().size());
    for (NameUsage u : resp.getResults()) {
      ids.put(u.getKey(), u.getScientificName());
      if (Origin.SOURCE == u.getOrigin()) {
        sourceCounter++;
      }
    }
    assertEquals(10, sourceCounter);

    // wait for 2 seconds, we allow a small time difference in old usage deletions
    Thread.sleep(2000);

    // 2nd import - there are 10 SOURCE usages with stable ids and 6 HIGHER usages with instable ids
    importer.run();
    resp = usageService.list(null, datasetKey, null, new PagingRequest(0, 100));
    assertEquals(16, resp.getResults().size());
    for (NameUsage u : resp.getResults()) {
      if (Origin.SOURCE == u.getOrigin()) {
        assertEquals(u.getScientificName(), ids.get(u.getKey()));
      } else {
        assertFalse("Usage key "+u.getKey()+" existed before", ids.containsKey(u.getKey()));
      }
    }
  }

}