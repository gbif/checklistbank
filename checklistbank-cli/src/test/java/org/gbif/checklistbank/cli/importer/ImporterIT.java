package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.BaseTest;
import org.gbif.checklistbank.cli.normalizer.NormalizerStats;
import org.gbif.checklistbank.cli.normalizer.NormalizerTest;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.index.guice.Solr;
import org.gbif.checklistbank.index.service.NameUsageSearchServiceImpl;
import org.gbif.checklistbank.nub.NubBuilder;
import org.gbif.checklistbank.nub.source.ClasspathSourceList;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.Mybatis;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.nub.lookup.straight.LookupUsage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Importer tests, using the normalizer test dwcas to first produce a neo4j db and then import that into postgres.
 * By default solr indexing is not tested and a mock service is used instead.
 * This is done cause neo4j uses an old version of lucene which conflicts with solr, preventing the use of an embedded solr server for tests.
 * An external solr instance can be configured manually in cfg-importer.yaml if wanted
 */
public class ImporterIT extends BaseTest implements AutoCloseable {

  private static final ObjectMapper CFG_MAPPER = new ObjectMapper(new YAMLFactory());
  private ImporterConfiguration iCfg;
  private NameUsageService nameUsageService;
  private UsageService usageService;
  private DatasetImportService sqlService;
  private DatasetImportService solrService;
  private NameUsageSearchService searchService;
  private HikariDataSource hds;

  @Rule
  public ClbDbTestRule dbSetup = ClbDbTestRule.empty();

  /**
   * Uses an internal metrics registry to setup the normalizer
   */
  public Importer build(ImporterConfiguration cfg, UUID datasetKey) throws SQLException {
    initGuice(cfg);
    return Importer.create(cfg, datasetKey, nameUsageService, usageService, sqlService, solrService);
  }

  private void initGuice(ImporterConfiguration cfg) {
    if (hds == null) {
      // init mybatis layer and solr from cfg instance
      Injector inj = Guice.createInjector(ChecklistBankServiceMyBatisModule.create(cfg.clb), new RealTimeModule(cfg.solr));
      hds = (HikariDataSource) inj.getInstance(InternalChecklistBankServiceMyBatisModule.DATASOURCE_KEY);
      nameUsageService = inj.getInstance(NameUsageService.class);
      usageService = inj.getInstance(UsageService.class);
      sqlService = inj.getInstance(Key.get(DatasetImportService.class, Mybatis.class));
      solrService = inj.getInstance(Key.get(DatasetImportService.class, Solr.class));
      if (!RealTimeModule.empty(iCfg.solr)) {
        searchService = new NameUsageSearchServiceImpl(inj.getInstance(SolrClient.class));
      }
    }
  }

  @Before
  public void initDwcaRepo() throws Exception {
    iCfg = CFG_MAPPER.readValue(Resources.getResource("cfg-importer.yaml"), ImporterConfiguration.class);
    iCfg.chunkMinSize=10;
    iCfg.chunkSize=50;
    iCfg.neo = cfg.neo;

    initGuice(iCfg);
    // truncate tables
    try (Connection con = hds.getConnection()) {
      try (Statement st = con.createStatement()) {
        st.execute("TRUNCATE citation CASCADE");
        st.execute("TRUNCATE name CASCADE");
      }
    }
  }

  @After
  public void close() throws Exception {
    if (sqlService != null) {
      sqlService.close();
    }
    if (solrService != null) {
      solrService.close();
    }
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
    PagingResponse<NameUsage> resp = nameUsageService.list(null, datasetKey, null, new PagingRequest(0, 500));
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
    PagingResponse<NameUsage> resp = nameUsageService.list(null, datasetKey, null, new PagingRequest(0, 100));
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
   * This test also checks solr if manually configured - default is without solr.
   */
  @Test
  public void testStableIds() throws Exception {
    final UUID datasetKey = NormalizerTest.datasetKey(14);

    // truncate solr
    solrService.deleteDataset(datasetKey);

    NameUsageSearchRequest search = new NameUsageSearchRequest();
    search.setLimit(1);
    search.setFacetLimit(100);
    search.addFacets(NameUsageSearchParameter.HIGHERTAXON_KEY);
    search.addChecklistFilter(datasetKey);
    if (!RealTimeModule.empty(iCfg.solr)) {
      // make sure there are no facets anymore
      Thread.sleep(1000);
      SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> srep = searchService.search(search);
      assertEquals(0, srep.getResults().size());
      assertEquals(0, srep.getFacets().get(0).getCounts().size());
    }

    // insert neo db
    insertNeo(datasetKey);

    // 1st import, keep neo db
    runImport(datasetKey);

    // check higher taxa
    // http://dev.gbif.org/issues/browse/POR-3204
    if (!RealTimeModule.empty(iCfg.solr)) {
      SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> srep = searchService.search(search);
      List<Facet.Count> facets = srep.getFacets().get(0).getCounts();
      // make sure the key actually exists!
      for (Facet.Count c : facets) {
        System.out.println(c);
        int key = Integer.valueOf(c.getName());
        NameUsage u = nameUsageService.get(key, null);
        assertNotNull("Higher taxon key "+key+" in solr does not exist in postgres", u);
      }
    }

    // remember ids
    Map<Integer, String> ids = Maps.newHashMap();
    int sourceCounter = 0;
    PagingResponse<NameUsage> resp = nameUsageService.list(null, datasetKey, null, new PagingRequest(0, 100));
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

    resp = nameUsageService.list(null, datasetKey, null, new PagingRequest(0, 100));
    assertEquals(16, resp.getResults().size());
    for (NameUsage u : resp.getResults()) {
      if (Origin.SOURCE == u.getOrigin()) {
        assertEquals(u.getScientificName(), ids.get(u.getKey()));
      } else {
        assertFalse("Usage key " + u.getKey() + " existed before", ids.containsKey(u.getKey()));
      }
    }

    // check higher taxa again, wait a little for solr to catch up
    if (!RealTimeModule.empty(iCfg.solr)) {
      Thread.sleep(1000);
      SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> srep = searchService.search(search);
      List<Facet.Count> facets = srep.getFacets().get(0).getCounts();
      for (Facet.Count c : facets) {
        System.out.println(c);
      }
      // make sure the key actually exists!
      for (Facet.Count c : facets) {
        System.out.println(c);
        int key = Integer.valueOf(c.getName());
        NameUsage u = nameUsageService.get(key, null);
        assertNotNull("Higher taxon key "+key+" in solr does not exist in postgres", u);
      }
    }
  }

  /**
   * Test richer nomenclatural data, make sure namePublishedIn is set.
   * See bottom comments on http://dev.gbif.org/issues/browse/POR-2480
   * See also http://dev.gbif.org/issues/browse/POR-3213
   */
  @Test
  public void testIndexFungorumNomen() throws Exception {
    final UUID datasetKey = NormalizerTest.datasetKey(6);

    // insert neo db
    NormalizerStats stats = insertNeo(datasetKey);
    assertEquals(1, stats.getRoots());
    assertEquals(290, stats.getCount());
    assertEquals(36, stats.getSynonyms());

    // 1st import
    runImport(datasetKey);

    // check first name
    NameUsage u426221 = getUsageByTaxonID(datasetKey, "426221");
    assertEquals("Führ. Pilzk. (Zwickau) 136 (1871)", u426221.getPublishedIn());


    PagingResponse<NameUsage> resp = nameUsageService.list(null, datasetKey, null, new PagingRequest(0, 200));

    NameUsage expected = null;
    for (NameUsage u : resp.getResults()) {
      if (u.getIssues().size() > 0) {
        System.err.println("Found "+u.toString());
        System.err.println("Issues are "+u.getIssues());
        expected = u;
        break;
      }
    }

    NameUsage same = nameUsageService.get(expected.getKey(), null);
    assertEquals(expected, same);
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
    assertEquals(20, stats.getCount());
    // 6+2 pro parte counts
    assertEquals(8, stats.getSynonyms());
    assertEquals(1, stats.getRoots());
    // pro parte records stay as one in the normalized neo db
    assertEquals(0, stats.getCountByOrigin(Origin.PROPARTE));
    assertEquals(20, stats.getCountByOrigin(Origin.SOURCE));

    // 1st import
    runImport(datasetKey);
    // verify
    verify16(datasetKey);

    // wait for 2 seconds, we allow a small time difference in old usage deletions
    Thread.sleep(2000);

    // 2nd import to make sure sync updates also work fine
    runImport(datasetKey);
    // verify
    verify16(datasetKey);
  }


  /**
   * Builds a small new nub and imports it, making sure the nub specific data gets through fine
   */
  @Test
  public void testNubImport() throws Exception {
    // build nub
    ClasspathSourceList src = ClasspathSourceList.source(3, 2, 15, 16, 51);
    src.setSourceRank(3, Rank.KINGDOM);
    openDb(Constants.NUB_DATASET_KEY);
    NubBuilder nb = NubBuilder.create(dao, src, IdLookupImpl.temp().load(Lists.<LookupUsage>newArrayList()), 10, 100);
    nb.run();
    dao.close();

    // import
    Importer imp = runImport(Constants.NUB_DATASET_KEY);
    assertEquals(67, imp.getSyncCounter());
    // make sure all usages have preassigned keys, not postgres generated ones!
    assertTrue(usageService.maxUsageKey(Constants.NUB_DATASET_KEY) < Constants.NUB_MAXIMUM_KEY);
    //test issue for 12 Neotetrastichodes flavus Girault, 1913 [synonym SPECIES] CONFLICTING_BASIONYM_COMBINATION
    NameUsage u = nameUsageService.listByCanonicalName(null, "Neotetrastichodes flavus", null, null).getResults().get(0);
    assertEquals("Neotetrastichodes flavus Girault, 1913", u.getScientificName());
    assertTrue(u.isSynonym());
    assertEquals("Aprostocetus rieki (De Santis, 1979)", u.getAccepted());

    NameUsage u2 = nameUsageService.listByCanonicalName(null, "Aprostocetus flavus", null, null).getResults().get(0);
    assertEquals("Aprostocetus flavus (Girault, 1913)", u2.getScientificName());
    assertTrue(u2.isSynonym());
    assertEquals(u.getAcceptedKey(), u2.getAcceptedKey());

    // make sure get does the same as list
    u2 = nameUsageService.get(u.getKey(), null);
    assertEquals(u, u2);

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
    assertTrue(usageService.maxUsageKey(datasetKey) > Constants.NUB_MAXIMUM_KEY);
  }

  private void verify16(UUID datasetKey) {
    PagingResponse<NameUsage> resp = nameUsageService.list(null, datasetKey, null, new PagingRequest(0, 100));
    // 18 source ones, 2 pro parte
    assertEquals(20, resp.getResults().size());
    int sources = 0;
    int proparte = 0;
    Map<String, String> proParteAcceptedNameGenusMap = Maps.newHashMap();
    proParteAcceptedNameGenusMap.put("Quertuga occidentalis", "Quertuga");
    proParteAcceptedNameGenusMap.put("Crepis occidentalis Nutt.", "Crepis");
    for (NameUsage u : resp.getResults()) {
      switch (u.getOrigin()) {
        case PROPARTE:
          proparte++;
          assertEquals("Leontodon occidentalis", u.getScientificName());
          assertNotNull(u.getGenusKey());
          assertNotNull(u.getAcceptedKey());
          assertTrue(u.isSynonym());
          assertNotNull(u.getFamilyKey());
          assertEquals("Asteraceae", u.getFamily());
          assertTrue(proParteAcceptedNameGenusMap.containsKey(u.getAccepted()));
          assertEquals(proParteAcceptedNameGenusMap.remove(u.getAccepted()), u.getGenus());
          break;
        case SOURCE:
          sources++;
          break;
        default:
          fail("Bad origin " + u.getOrigin());
      }
      assertNotNull(u.toString(), u.getKingdomKey());
      assertNotNull(u.toString(), u.getKingdom());
      if (Rank.KINGDOM != u.getRank()) {
        assertNotNull(u.toString(), u.getFamilyKey());
        assertNotNull(u.toString(), u.getFamily());
        if (u.isSynonym()) {
          assertNotNull(u.toString(), u.getAcceptedKey());
          assertNotNull(u.toString(), u.getAccepted());
        } else {
          assertNotNull(u.toString(), u.getParentKey());
          assertNotNull(u.toString(), u.getParent());
        }
      }
    }
    assertEquals(18, sources);
    assertEquals(2, proparte);
    assertTrue(proParteAcceptedNameGenusMap.isEmpty());

    NameUsage u = getUsageByTaxonID(datasetKey, "1001");
    assertNotNull(u.getBasionymKey());
    assertEquals("Kreps bakeri DC.", u.getBasionym());

    u = getUsageByTaxonID(datasetKey, "1002");
    assertNotNull(u.getBasionymKey());
    assertEquals("Leontodon occidentalis", u.getBasionym());
    assertEquals("Crepis", u.getParent());
    assertEquals("Crepis", u.getGenus());
    assertEquals("Asteraceae", u.getFamily());

    u = getUsageByTaxonID(datasetKey, "1006-s1");
    assertTrue(u.isSynonym());
    assertNotNull(u.getAcceptedKey());
    assertEquals("Leontodon taraxacoides (Vill.) Mérat", u.getAccepted());
    assertEquals("Leontodon", u.getGenus());
    assertEquals("Asteraceae", u.getFamily());
  }

  private NameUsage getUsageByTaxonID(UUID datasetKey, String taxonID) {
    PagingResponse<NameUsage> resp = nameUsageService.list(null, datasetKey, taxonID, null);
    assertEquals("More than one usage have the taxonID " + taxonID, 1, resp.getResults().size());
    return resp.getResults().get(0);
  }

  private Importer runImport(UUID datasetKey) throws SQLException {
    Importer importer = build(iCfg, datasetKey);
    importer.run();
    return importer;
  }
}