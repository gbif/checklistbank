package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestResult;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.api.vocabulary.Habitat;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.api.vocabulary.ThreatStatus;
import org.gbif.checklistbank.index.IndexingTestModule;
import org.gbif.checklistbank.index.NameUsageIndexer;
import org.gbif.checklistbank.index.guice.EmbeddedSolrReference;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;
import org.gbif.checklistbank.ws.UrlBindingModule;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankSearchWsTestModule;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWsClientModule;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.client.BaseResourceTest;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_LIMIT;
import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_OFFSET;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NameUsageSearchWsClientIT extends BaseResourceTest {

  private static final String PROPERTY_INDEXER_DEFAULT = "checklistbank-indexer-default.properties";

  private static final Logger LOG = LoggerFactory.getLogger(NameUsageSearchWsClientIT.class);

  private NameUsageSearchService wsClient;
  private static EmbeddedSolrReference solrRef;

  /**
   * Starts up a solr server and indexes the test database.
   * Wrapped in a static method so we can set the solr server in the ChecklistBankSearchWsTestModule
   * which must have a default, empty constructor.
   */
  public static EmbeddedSolrReference setup() {
    if (solrRef != null) {
      return solrRef;
    }

    // run liquibase & dbunit
    LOG.info("Run liquibase & dbunit once");
    try {
      DatabaseDrivenChecklistBankTestRule rule = new DatabaseDrivenChecklistBankTestRule(null);
      rule.apply(new Statement() {
        public void evaluate() throws Throwable {
          // do nothing
        }
      }, null).evaluate();
    } catch (Throwable throwable) {
      Throwables.propagate(throwable);
    }

    // Creates the injector, merging properties taken from default test indexing and checklistbank
    try {
      Properties props = PropertiesUtil.loadProperties(PROPERTY_INDEXER_DEFAULT);
      Properties props2 = PropertiesUtil.loadProperties(ClientMyBatisITBase.PROPERTIES_FILE);
      props.putAll(props2);
      Injector injector = Guice.createInjector(new IndexingTestModule(props));

      // Gets the indexer instance
      solrRef = injector.getInstance(EmbeddedSolrReference.class);

      // build the solr index
      NameUsageIndexer nameUsageIndexer = injector.getInstance(NameUsageIndexer.class);
      LOG.info("Build solr index");
      nameUsageIndexer.run();

      return solrRef;

    } catch (IOException e) {
      throw new RuntimeException("Cant load properties to build solr index", e);
    }
  }


  public NameUsageSearchWsClientIT() {
    super("org.gbif.checklistbank.ws", ClientMyBatisITBase.CONTEXT, ChecklistBankSearchWsTestModule.class);
  }

  @Before
  public void init() {
    Properties props;
    try {
      props = PropertiesUtil.loadProperties(ClientMyBatisITBase.PROPERTIES_FILE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Injector clientInjector = Guice.createInjector(new UrlBindingModule(getBaseURI(), ClientMyBatisITBase.CONTEXT),
      new ChecklistBankWsClientModule(props, true, false));
    wsClient = clientInjector.getInstance(NameUsageSearchService.class);
  }

  @Test
  public void searchHighlightTest() {
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(DEFAULT_PARAM_OFFSET, DEFAULT_PARAM_LIMIT);
    searchRequest.setQ("puma");
    searchRequest.setHighlight(true);
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response = wsClient.search(searchRequest);
    assertNotNull(response);
  }

  @Test
  public void searchSuggestTest() {
    NameUsageSuggestRequest searchSuggestRequest = new NameUsageSuggestRequest();
    searchSuggestRequest.setQ("tetracanthus");
    List<NameUsageSuggestResult> results = wsClient.suggest(searchSuggestRequest);
    assertNotNull(results);
  }

  @Test
  public void searchTest() {
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(DEFAULT_PARAM_OFFSET, DEFAULT_PARAM_LIMIT);
    searchRequest.setQ("puma");
    searchRequest.addFacets(NameUsageSearchParameter.DATASET_KEY);
    searchRequest.addParameter(NameUsageSearchParameter.DATASET_KEY, "d7dddbf4-2cf0-4f39-9b2a-bb099caae36c");
    searchRequest.setMultiSelectFacets(true);
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response = wsClient.search(searchRequest);
    assertNotNull(response);
  }

  @Test
  public void searchTestWithNubKey() {
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(DEFAULT_PARAM_OFFSET, DEFAULT_PARAM_LIMIT);
    searchRequest.setQ("oenanthe");
    searchRequest.addParameter(NameUsageSearchParameter.HIGHERTAXON_KEY, "3184223");
    searchRequest.addFacets(NameUsageSearchParameter.DATASET_KEY);
    searchRequest.addParameter(NameUsageSearchParameter.DATASET_KEY, "d7dddbf4-2cf0-4f39-9b2a-bb099caae36c");
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response = wsClient.search(searchRequest);
    assertNotNull(response);
  }

  @Test
  public void searchSearches() {
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> result =
      assertSearch("vulgaris", NameUsageSearchParameter.RANK, 10L, null);
  }

  @Test
  public void searchSearchFacets() {
    // RANK
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> result =
      assertSearch("Sciurus vulgaris", NameUsageSearchParameter.RANK, 10L, null);
    assertSearch("Sciurus vulgaris", NameUsageSearchParameter.RANK, Rank.VARIETY, 2l, null);
    result = assertSearch("Sciurus vulgaris", NameUsageSearchParameter.RANK, Rank.SPECIES, 1l, null);
    assertEquals((Integer) 100000025, result.getResults().get(0).getKey());
    assertEquals("Sciurus vulgaris Linnaeus, 1758", result.getResults().get(0).getScientificName());

    // CHECKLIST
     assertSearch("Animalia", NameUsageSearchParameter.DATASET_KEY, 46L, null);
    result = assertSearch("Animalia", NameUsageSearchParameter.DATASET_KEY, Constants.NUB_DATASET_KEY.toString(), 2l, null);
    assertEquals((Integer) 1, result.getResults().get(0).getKey());
    assertSearch("Animalia", NameUsageSearchParameter.DATASET_KEY, UUID.randomUUID().toString(), 0l, null);

    // HIGHERTAXON
    assertSearch("Rodentia", NameUsageSearchParameter.HIGHERTAXON_KEY, 42L, null);
    // we only have the order Rodentia in the nub without children
    result = assertSearch("Rodentia", NameUsageSearchParameter.HIGHERTAXON_KEY, "1", 1l, null);
    assertEquals((Integer) 10, result.getResults().get(0).getKey());
    // we have the order Rodentia in the checklist linked to subgenus Scirius (100000024) with 14 children
    assertSearch("Rodentia", NameUsageSearchParameter.HIGHERTAXON_KEY, "100000024", 14l, null);

    // TAXSTATUS
    assertSearch("Sciurus", NameUsageSearchParameter.STATUS, 30L, null);
    assertSearch("Sciurus", NameUsageSearchParameter.STATUS, TaxonomicStatus.SYNONYM, 15l, null);
    assertSearch("Sciurus", NameUsageSearchParameter.STATUS, TaxonomicStatus.HETEROTYPIC_SYNONYM, 1l, null);
    assertSearch("Sciurus", NameUsageSearchParameter.STATUS, TaxonomicStatus.MISAPPLIED, 0l, null);

    // EXTINCT
    assertSearch("Sciurus", NameUsageSearchParameter.IS_EXTINCT, 30L, null);
    result = assertSearch("Sciurus", NameUsageSearchParameter.IS_EXTINCT, "false", 1l, null);
    assertEquals((Integer) 100000025, result.getResults().get(0).getKey());
    assertSearch("Sciurus", NameUsageSearchParameter.IS_EXTINCT, "true", 1l, null);

    // HABITAT
    assertSearch("Sciurus", NameUsageSearchParameter.HABITAT, 30L, null);
    result = assertSearch("Sciurus", NameUsageSearchParameter.HABITAT, Habitat.TERRESTRIAL, 1l, null);
    assertEquals((Integer) 100000025, result.getResults().get(0).getKey());
    assertSearch("Sciurus", NameUsageSearchParameter.HABITAT, Habitat.MARINE, 0l, null);
    assertSearch("Sciurus", NameUsageSearchParameter.HABITAT, Habitat.FRESHWATER, 1l, null);

    // THREAT
    assertSearch("Sciurillus", NameUsageSearchParameter.THREAT, 5L, null);
    result = assertSearch("Sciurillus", NameUsageSearchParameter.THREAT, ThreatStatus.NEAR_THREATENED, 1l, null);
    assertEquals((Integer) 100000007, result.getResults().get(0).getKey());
    assertSearch("Sciurillus", NameUsageSearchParameter.THREAT, ThreatStatus.EXTINCT_IN_THE_WILD, 0l, null);

    // HIGHERTAXON & RANK
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(0L, 20);
    searchRequest.setQ("Sciurus");
    searchRequest.addFacets(NameUsageSearchParameter.HIGHERTAXON_KEY, NameUsageSearchParameter.RANK);
    assertSearch(searchRequest, true, 30l, null);

    searchRequest.addParameter(NameUsageSearchParameter.HIGHERTAXON_KEY, "100000025");
    assertSearch(searchRequest, true, 13l, null);

    searchRequest.addParameter(NameUsageSearchParameter.RANK, Rank.VARIETY);
    assertSearch(searchRequest, true, 2l, null);
  }


  private SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> assertSearch(String q,
    NameUsageSearchParameter facet,
    Long expectedCount, String expectedFacetCounts) {
    return assertSearch(q, facet, (String) null, expectedCount, expectedFacetCounts);
  }

  private SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> assertSearch(String q,
    NameUsageSearchParameter facet,
    Enum<?> facetFilter, Long expectedCount, String expectedFacetCounts) {

    NameUsageSearchRequest searchRequest = prepareRequest(q, facet);
    if (facetFilter != null) {
      searchRequest.addParameter(facet, facetFilter);
    }

    return assertSearch(searchRequest, facet != null, expectedCount, expectedFacetCounts);
  }

  private SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> assertSearch(String q,
    NameUsageSearchParameter facet,
    String facetFilter, Long expectedCount, String expectedFacetCounts) {

    NameUsageSearchRequest searchRequest = prepareRequest(q, facet);
    if (facetFilter != null) {
      searchRequest.addParameter(facet, facetFilter);
    }
    return assertSearch(searchRequest, facet != null, expectedCount, expectedFacetCounts);
  }

  private NameUsageSearchRequest prepareRequest(String q, NameUsageSearchParameter facet) {
    // build request
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(0L, 20);
    searchRequest.setQ(q);

    if (facet != null) {
      searchRequest.addFacets(facet);
    }

    return searchRequest;
  }

  private SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> assertSearch(
    NameUsageSearchRequest searchRequest, boolean useFacets,
    Long expectedCount, String expectedFacetCounts) {

    // query
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response = wsClient.search(searchRequest);

    // assert
    if (expectedCount != null) {
      assertEquals(expectedCount, response.getCount());
    }

    if (useFacets && expectedFacetCounts != null) {
      assertEquals(1, response.getFacets().size());
      assertEquals(expectedFacetCounts, response.getFacets().get(0).getCounts().size());
    }
    if (!useFacets) {
      Assert.assertTrue(response.getFacets().isEmpty());
    }

    for (NameUsageSearchResult u : response.getResults()) {
      assertNotNull(u);
      assertEquals(NameUsageSearchResult.class, u.getClass());
    }

    return response;
  }
}
