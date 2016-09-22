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

import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_LIMIT;
import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_OFFSET;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NameUsageSearchWsClientIT {
  private static String SQUIRRELS_DATASET_KEY = "109aea14-c252-4a85-96e2-f5f4d5d088f4";

  private NameUsageSearchService wsClient;

  public NameUsageSearchWsClientIT() {
    wsClient = WsClientSuite.getClient(NameUsageSearchService.class);
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
    NameUsageSuggestRequest req = new NameUsageSuggestRequest();
    req.addParameter(NameUsageSearchParameter.DATASET_KEY, SQUIRRELS_DATASET_KEY);
    req.setLimit(100);
    req.setQ("tetracanthus");
    List<NameUsageSuggestResult> results = wsClient.suggest(req);
    for (NameUsageSuggestResult r : results) {
      System.out.println(r.getScientificName());
    }
    assertEquals(0, results.size());
    assertNotNull(results);

    req = new NameUsageSuggestRequest();
    req.addParameter(NameUsageSearchParameter.DATASET_KEY, SQUIRRELS_DATASET_KEY);
    req.setLimit(100);
    req.setQ("Sciu");
    results = wsClient.suggest(req);

    //TODO: why does this not work !!!???!??!?!?!
//    assertEquals(27, results.size());
//    assertEquals("Sciuridae", results.get(0).getScientificName());
  }

  @Test
  public void searchTest() {
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(DEFAULT_PARAM_OFFSET, DEFAULT_PARAM_LIMIT);
    searchRequest.setQ("puma");
    searchRequest.addFacets(NameUsageSearchParameter.DATASET_KEY);
    searchRequest.addParameter(NameUsageSearchParameter.DATASET_KEY, Constants.NUB_DATASET_KEY.toString());
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
    searchRequest.addParameter(NameUsageSearchParameter.DATASET_KEY, Constants.NUB_DATASET_KEY.toString());
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response = wsClient.search(searchRequest);
    assertNotNull(response);
  }

  @Test
  public void searchSearches() {
    assertSearch("vulgaris", NameUsageSearchParameter.RANK, 10L, null);
  }

  @Test
  public void searchSearchFacets() {
    // RANK
    assertSearch("Sciurus vulgaris", NameUsageSearchParameter.RANK, 10L, null);
    assertSearch("Sciurus vulgaris", NameUsageSearchParameter.RANK, Rank.VARIETY, 2l, null);

    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> result = assertSearch("Sciurus vulgaris", NameUsageSearchParameter.RANK, Rank.SPECIES, 1l, null);
    assertEquals((Integer) 100000025, result.getResults().get(0).getKey());
    assertEquals("Sciurus vulgaris Linnaeus, 1758", result.getResults().get(0).getScientificName());

    // CHECKLIST
    assertSearch("Animalia", NameUsageSearchParameter.DATASET_KEY, 2L, null);
    result = assertSearch("Animalia", NameUsageSearchParameter.DATASET_KEY, Constants.NUB_DATASET_KEY.toString(), 1l, null);
    assertEquals((Integer) 1, result.getResults().get(0).getKey());
    assertSearch("Animalia", NameUsageSearchParameter.DATASET_KEY, UUID.randomUUID().toString(), 0l, null);

    // HIGHERTAXON
    assertSearch("Rodentia", NameUsageSearchParameter.HIGHERTAXON_KEY, 2L, null);
    // we only have the order Rodentia in the nub without children
    result = assertSearch("Rodentia", NameUsageSearchParameter.HIGHERTAXON_KEY, "1", 1l, null);
    assertEquals((Integer) 10, result.getResults().get(0).getKey());

    // plain facet filter search, no query
    NameUsageSearchRequest req = new NameUsageSearchRequest();
    req.setQ(null);
    req.addChecklistFilter(UUID.fromString(SQUIRRELS_DATASET_KEY));
    req.addHigherTaxonFilter(100000024);
    assertSearch(req, true, 14l, null);

    // TAXSTATUS
    assertSearch("Sciurus", NameUsageSearchParameter.STATUS, 17L, null);
    assertSearch("Sciurus", NameUsageSearchParameter.STATUS, TaxonomicStatus.SYNONYM, 4l, null);
    assertSearch("Sciurus", NameUsageSearchParameter.STATUS, TaxonomicStatus.HETEROTYPIC_SYNONYM, 0l, null);
    assertSearch("Sciurus", NameUsageSearchParameter.STATUS, TaxonomicStatus.MISAPPLIED, 0l, null);

    // EXTINCT
    assertSearch("Sciurus", NameUsageSearchParameter.IS_EXTINCT, 17L, null);
    result = assertSearch("Sciurus", NameUsageSearchParameter.IS_EXTINCT, "false", 1l, null);
    assertEquals((Integer) 100000025, result.getResults().get(0).getKey());
    assertSearch("Sciurus", NameUsageSearchParameter.IS_EXTINCT, "true", 1l, null);

    // HABITAT
    assertSearch("Sciurus", NameUsageSearchParameter.HABITAT, 17L, null);
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
    assertSearch(searchRequest, true, 17L, null);

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

  private SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> assertSearch(String q, NameUsageSearchParameter facet,
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

  private SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> assertSearch(NameUsageSearchRequest searchRequest, boolean useFacets,
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
