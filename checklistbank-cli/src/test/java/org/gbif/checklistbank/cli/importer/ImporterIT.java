package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.normalizer.NeoTest;
import org.gbif.checklistbank.cli.normalizer.Normalizer;
import org.gbif.checklistbank.cli.normalizer.NormalizerConfiguration;
import org.gbif.checklistbank.cli.normalizer.NormalizerTest;
import org.gbif.checklistbank.index.NameUsageIndexService;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.service.DatasetImportService;
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
import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.jvm.MemoryUsageGaugeSet;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Importer tests, using the normalizer test dwcas to first produce a neo4j db and then import that into postgres.
 */
public class ImporterIT extends NeoTest {

  private static final ObjectMapper CFG_MAPPER = new ObjectMapper(new YAMLFactory());
  private NormalizerConfiguration nCfg;
  private ImporterConfiguration iCfg;
  private NameUsageService usageService;
  private DatasetImportServiceCombined importService;
  private HikariDataSource hds;
  public ImporterIT() {
    super(false);
  }

  /**
   * Uses an internal metrics registry to setup the normalizer
   */
  public Importer build(ImporterConfiguration cfg, UUID datasetKey) throws SQLException {
    MetricRegistry registry = new MetricRegistry("normalizer");
    MemoryUsageGaugeSet mgs = new MemoryUsageGaugeSet();
    registry.registerAll(mgs);

    registry.meter(ImporterService.SYNC_METER);

    initGuice(cfg);
    return new Importer(cfg, datasetKey, registry, importService, usageService);
  }

  private void initGuice(ImporterConfiguration cfg) {
    if (hds == null) {
      // init mybatis layer and solr from cfg instance
      Injector inj = Guice.createInjector(cfg.clb.createServiceModule(), new RealTimeModule(cfg.solr));
      Key<DataSource> dsKey = Key.get(DataSource.class, Names.named(InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME));
      hds = (HikariDataSource) inj.getInstance(dsKey);
      usageService = inj.getInstance(NameUsageService.class);
      importService = new DatasetImportServiceCombined(inj.getInstance(DatasetImportService.class), inj.getInstance(NameUsageIndexService.class));
    }
  }

  @Before
  public void initDwcaRepo() throws Exception {
    nCfg = new NormalizerConfiguration();
    nCfg.neo = super.cfg;

    URL dwcasUrl = getClass().getResource("/dwcas");
    Path p = Paths.get(dwcasUrl.toURI());
    nCfg.archiveRepository = p.toFile();

    iCfg = CFG_MAPPER.readValue(Resources.getResource("cfg-importer.yaml"), ImporterConfiguration.class);
    iCfg.neo = nCfg.neo;
    iCfg.clb.serverName="127.0.0.1";
    iCfg.clb.databaseName="clb";
    iCfg.clb.user="postgres";
    iCfg.clb.password="pogo";

    initGuice(iCfg);
    // truncate tables
    try (Connection con = hds.getConnection()){
      try (Statement st = con.createStatement()){
        st.execute("TRUNCATE citation CASCADE");
        st.execute("TRUNCATE name CASCADE");
      }
    }
  }

  @After
  public void shutdownPool() throws Exception {
    if (hds != null) {
      hds.close();
    }
  }

  @Test
  public void testIdList() throws SQLException {
    final UUID datasetKey = NormalizerTest.datasetKey(1);

    // insert neo db
    insertNeo(datasetKey);

    // import
    runImport(datasetKey);

    // test db, all usages must be accepted and there is one root!
    PagingResponse<NameUsage> resp = usageService.list(null, datasetKey, null, new PagingRequest(0, 500));
    assertEquals(20, resp.getResults().size());
    for (NameUsage u : resp.getResults()) {
      assertEquals("Bad datasetKey", datasetKey, u.getDatasetKey());
      if (u.isSynonym()) {
        assertNotNull(u.getAcceptedKey());
        assertNotNull(u.getAccepted());
      } else {
        assertNull(u.getAcceptedKey());
        assertNull(u.getAccepted());
      }
      if (u.getRank() != Rank.KINGDOM) {
        assertNotNull(u.getParentKey());
        assertNotNull(u.getParent());
      }
      assertNotNull(u.getOrigin());
      assertNotNull(u.getRank());
      if (u.getRank().isLinnean()) {
        if (u.isSynonym()) {
          assertFalse("Higher classification key for synonym " + u.getScientificName() + " cannot point to itself!",
            u.getKey().equals(ClassificationUtils.getHigherRankKey(u, u.getRank())));
        } else {
          assertEquals("Bad higher classification key for " + u.getScientificName() + " of rank " + u.getRank(),
            u.getKey(), ClassificationUtils.getHigherRankKey(u, u.getRank()));
        }
      }
    }
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
    final UUID datasetKey = NormalizerTest.datasetKey(14);

    // insert neo db
    insertNeo(datasetKey);

    // import
    runImport(datasetKey);

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
      assertNotNull(u.getRank());
      if (u.getScientificName().equals("Animalia")) {
        assertNull(u.getParentKey());
        assertNull(u.getParent());
      } else {
        assertNotNull(u.getParentKey());
        assertNotNull(u.getParent());
      }
      if (u.getRank().isLinnean()) {
        assertEquals("Bad higher classification key for " + u.getScientificName() + " of rank " + u.getRank(),
          u.getKey(), ClassificationUtils.getHigherRankKey(u, u.getRank()));
      }
    }
  }

  /**
   * Reimport the same dataset and make sure ids stay the same.
   */
  @Test
  public void testStableIds() throws Exception {
    final UUID datasetKey = NormalizerTest.datasetKey(14);

    // insert neo db
    insertNeo(datasetKey);

    // 1st import, keep neo db
    runImport(datasetKey);
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
    runImport(datasetKey);

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

  /**
   * Test richer nomenclatural data, make sure namePublishedIn is set.
   * See bottom comments on http://dev.gbif.org/issues/browse/POR-2480
   */
  @Test
  public void testIndexFungorumNomen() throws Exception {
    final UUID datasetKey = NormalizerTest.datasetKey(6);

    // insert neo db
    NormalizerStats stats = insertNeo(datasetKey);
    assertEquals(1, stats.getRoots());
    assertEquals(296, stats.getCount());
    assertEquals(36, stats.getSynonyms());

    // 1st import
    runImport(datasetKey);

    // check first name
    NameUsage u = getUsageByTaxonID(datasetKey, "426221");
    assertEquals("Führ. Pilzk. (Zwickau) 136 (1871)", u.getPublishedIn());
  }

  /**
   * Import a dataset that has basionym & proparte links to not previously imported usages.
   * We need to post update those foreign keys after all records have been inserted!
   */
  @Test
  public void testMissingUsageKeys() throws Exception {
    final UUID datasetKey = NormalizerTest.datasetKey(16);

    // insert neo db
    NormalizerStats stats = insertNeo(datasetKey);
    assertEquals(17, stats.getCount());
    assertEquals(7, stats.getSynonyms());
    assertEquals(1, stats.getRoots());
    assertEquals(1, stats.getCountByOrigin(Origin.PROPARTE));
    assertEquals(16, stats.getCountByOrigin(Origin.SOURCE));

    // 1st import
    runImport(datasetKey);
    // verify
    verify14(datasetKey);

    // wait for 2 seconds, we allow a small time difference in old usage deletions
    Thread.sleep(2000);

    // 2nd import to make sure sync updates also work fine
    runImport(datasetKey);
    // verify
    verify14(datasetKey);
  }


  /**
   * http://dev.gbif.org/issues/browse/POR-2755
   */
  @Test
  public void testMissingGenusFloraBrazil() throws Exception {
    final UUID datasetKey = NormalizerTest.datasetKey(19);

    // insert neo db
    NormalizerStats stats = insertNeo(datasetKey);
    assertEquals(3, stats.getRoots());
    assertEquals(151, stats.getCount());
    assertEquals(62, stats.getSynonyms());
    assertEquals(3, stats.getCountByOrigin(Origin.VERBATIM_PARENT));
    assertEquals(1, stats.getCountByOrigin(Origin.VERBATIM_ACCEPTED));
    assertEquals(60, stats.getCountByOrigin(Origin.MISSING_ACCEPTED));
    assertEquals(87, stats.getCountByOrigin(Origin.SOURCE));

    // 1st import
    runImport(datasetKey);
  }

  private void verify14(UUID datasetKey) {
    PagingResponse<NameUsage> resp = usageService.list(null, datasetKey, null, new PagingRequest(0, 100));
    assertEquals(17, resp.getResults().size());
    for (NameUsage u : resp.getResults()) {
      if (Rank.KINGDOM != u.getRank()) {
        if (u.isSynonym()) {
          assertNotNull(u.getAcceptedKey());
          assertNotNull(u.getAccepted());
        } else {
          assertNotNull(u.getParentKey());
          assertNotNull(u.getParent());
        }
      }
    }
    NameUsage u = getUsageByTaxonID(datasetKey, "1001");
    assertNotNull(u.getBasionymKey());
    assertEquals("Kreps bakeri DC.", u.getBasionym());

    u = getUsageByTaxonID(datasetKey, "1002");
    assertNotNull(u.getBasionymKey());
    assertEquals("Leontodon occidentalis", u.getBasionym());
  }

  private NameUsage getUsageByTaxonID(UUID datasetKey, String taxonID) {
    PagingResponse<NameUsage> resp = usageService.list(null, datasetKey, taxonID, null);
    assertEquals("More than one usage have the taxonID "+taxonID, 1, resp.getResults().size());
    return resp.getResults().get(0);
  }

  private Importer runImport(UUID datasetKey) throws SQLException {
    Importer importer = build(iCfg, datasetKey);
    importer.run();
    return importer;
  }

  private NormalizerStats insertNeo(UUID datasetKey) {
    Normalizer norm = NormalizerTest.buildNormalizer(nCfg, datasetKey);
    norm.run();
    NormalizerStats stats = norm.getStats();

    initDb(datasetKey, stats);
    db.shutdown();
    return stats;
  }
}