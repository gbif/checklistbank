/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.index.service;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.checklistbank.search.*;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.index.backfill.SolrTestSetup;
import org.gbif.checklistbank.index.guice.SearchTestModule;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.gbif.common.search.solr.SolrConstants;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests using an embedded solr server with the mybatis squirrels test dataset.
 * The solr index will be rebuild before the test using the NameUsageIndexerBaseIT base class.
 */
public class NameUsageSearchServiceIT {

  protected final Logger log = LoggerFactory.getLogger(NameUsageSearchServiceIT.class);
  private static final String PROPERTY_FILE = "checklistbank.properties";
  private static NameUsageSearchService searchService;
  private static String SQUIRRELS_DATASET_KEY = "109aea14-c252-4a85-96e2-f5f4d5d088f4";

  @BeforeClass
  public static void setup() throws Exception {
    // creates squirrels db and solr index & server using its own injector
    SolrTestSetup setup = new SolrTestSetup(ClbDbTestRule.squirrels());
    setup.setup();

    // insert new injector for this test, reusing existing solr server
    Properties props = PropertiesUtil.loadProperties(PROPERTY_FILE);
    Injector injector = Guice.createInjector(new SearchTestModule(props, setup.solr()));

    searchService = injector.getInstance(NameUsageSearchService.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadFilterRequest() {
    // it should be a known rank enum name
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(0L, 10);
    searchRequest.addParameter(NameUsageSearchParameter.RANK, "1");
    searchService.search(searchRequest);
  }

  @Test
  public void testEnumFilterRequest() {
    // test good query with a rank enum name
    NameUsageSearchRequest req = new NameUsageSearchRequest(0L, 25);
    req.addParameter(NameUsageSearchParameter.RANK, "order");
    assertSearch(req, 2l, null);

    req.addParameter(NameUsageSearchParameter.RANK, "genus");
    assertSearch(req, 16l, null);
  }

  @Test
  public void testNegatedFilters() {
    // test good query with a rank enum name
    NameUsageSearchRequest req = new NameUsageSearchRequest(0L, 50);
    req.addParameter(NameUsageSearchParameter.RANK, "!genus");
    req.addParameter(NameUsageSearchParameter.RANK, "!species");
    req.addParameter(NameUsageSearchParameter.RANK, "!subspecies");

    assertSearch(req, 15l, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegatedFiltersError() {
    // test good query with a rank enum name
    NameUsageSearchRequest req = new NameUsageSearchRequest(0L, 50);
    req.addParameter(NameUsageSearchParameter.RANK, "!genus");
    req.addParameter(NameUsageSearchParameter.RANK, "species");
    assertSearch(req, null, null);
  }

  @Test
  public void testSearchScientificNameNoFacets() {
    assertSearch("Rodentia", 2L);
    assertSearch("Rodentia Bowdich", 2L);
    assertSearch("Rodentia Bowdich, 1821", 2L);
    assertSearch("Rodentia Bowdich 1821", 2L);
    assertSearch("Puma concolor, 1821", 0L);
    assertSearch("vulgaris", 10L);
    assertSearch("Sciurus", 17L);
    assertSearch("Sciurillus pusillus", 4L);
  }

  @Test
  public void testSearchScientificNameWithRankFacet() {
    assertSearch("vulgaris", NameUsageSearchParameter.RANK, null, 10L, null);
    assertSearch("Sciurus vulgaris", NameUsageSearchParameter.RANK, null, 10L, null);
    assertSearch("Sciurus vulgaris", NameUsageSearchParameter.RANK, Rank.SPECIES, 1l, null);
    assertSearch("Sciurus vulgaris", NameUsageSearchParameter.RANK, Rank.VARIETY, 2l, null);

    assertSearch("Sciurus vulgaris", NameUsageSearchParameter.RANK, Rank.VARIETY, 2l, null);
  }

  @Test
  public void testNomStatusFacet() {
    assertSearch(null, NameUsageSearchParameter.NOMENCLATURAL_STATUS, NomenclaturalStatus.NUDUM, 1L, 2);
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> resp = search(null, NameUsageSearchParameter.NOMENCLATURAL_STATUS, NomenclaturalStatus.NUDUM);
    assertEquals((Integer) 100000026, resp.getResults().get(0).getKey());
  }

  @Test
  public void testHigherTaxonFilter() {
    NameUsageSearchRequest req = new NameUsageSearchRequest();
    req.addChecklistFilter(UUID.fromString(SQUIRRELS_DATASET_KEY));
    req.addHigherTaxonFilter(100000024);

    assertSearch(req, 14L, null);
  }


  @Test
  public void testSuggest() {
    List<NameUsageSuggestResult> results = searchSuggest("sci");
    assertEquals(27, results.size());
    // highest rank first
    assertEquals("Sciuromorpha Brandt, 1855", results.get(0).getScientificName());

    // match start of later epithets
    results = searchSuggest("alpin");
    assertEquals(1, results.size());
    assertEquals("Sciurus vulgaris var. alpinus Desmarest, 1822", results.get(0).getScientificName());

    // match entire epithets, highest rank first
    results = searchSuggest("vulgaris");
    assertEquals(10, results.size());
    assertEquals("Sciurus vulgaris Linnaeus, 1758", results.get(0).getScientificName());

    // https://github.com/gbif/checklistbank/issues/11
    assertSciurusVulgaris("Sciurus v");
    assertSciurusVulgaris("Sciurus vu");
    assertSciurusVulgaris("Sciurus vul");
    assertSciurusVulgaris("Sciurus vulg");
    assertSciurusVulgaris("Sciurus vulga");
    assertSciurusVulgaris("Sciurus vulgar");
    assertSciurusVulgaris("Sciurus vulgari");
    assertSciurusVulgaris("Sciurus vulgaris");

    // only match squirrel dataset
    results = searchSuggest("Roden");
    assertEquals(1, results.size());
    assertEquals("Rodentia Bowdich, 1821", results.get(0).getScientificName());

    results = searchSuggest("Sciurillus pusillus");
    assertEquals(4, results.size());
    assertEquals("Sciurillus pusillus E. Geoffroy, 1803", results.get(0).getScientificName());

    // we changed the gender epithets, no stemming in the index anymore
    assertNoSuggest("Sciurilus pusila");
    assertNoSuggest("Sciurilus pussy");
    assertNoSuggest("Sciurilus pusilus");

    results = searchSuggest("Sciu");
    assertEquals(27, results.size());
    assertEquals("Sciuromorpha Brandt, 1855", results.get(0).getScientificName());

  }

  private void assertSciurusVulgaris(String q) {
    List<NameUsageSuggestResult> results = searchSuggest(q);
    assertEquals(10, results.size());
    assertEquals("Sciurus vulgaris Linnaeus, 1758", results.get(0).getScientificName());
  }

  private void assertNoSuggest(String q) {
    List<NameUsageSuggestResult> results = searchSuggest(q);
    assertTrue(results.isEmpty());
  }

  @Test
  public void testHighlighting() {
    // build request
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(0L, 10);
    searchRequest.setQ("Eichhörnchen");
    searchRequest.setHighlight(true);
    searchRequest.setExtended(false);
    searchRequest.setHighlightContext(5);

    // query with extended=false
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response = searchService.search(searchRequest);
    assertEquals((Long) 2l, response.getCount());
    // this usage also matches description and should therefore come first
    assertEquals((Integer) 100000040, response.getResults().get(0).getKey());

    // assert vernaculars
    List<VernacularName> names = response.getResults().get(0).getVernacularNames();
    assertEquals(1, names.size());
    assertEquals(1, countHighlights(names, getVernacular));

    // assert descriptions
    List<Description> descriptions = response.getResults().get(0).getDescriptions();
    assertEquals(1, countHighlights(descriptions, getDescriptions));


    // query extended, but hl description only
    searchRequest.setExtended(true);
    searchRequest.setHighlightFields(Sets.newHashSet(NameUsageSearchRequest.QueryField.DESCRIPTION));

    response = searchService.search(searchRequest);

    // assert 2 vernacular names without highlighting
    names = response.getResults().get(0).getVernacularNames();
    assertEquals(2, names.size());
    assertEquals(0, countHighlights(names, getVernacular));

    // assert descriptions
    descriptions = response.getResults().get(0).getDescriptions();
    assertEquals(3, descriptions.size());
    assertEquals(1, countHighlights(descriptions, getDescriptions));
  }

  private static <T> int countHighlights(List<T> objs, Function<T, String> getText) {
    int cnt = 0;
    for (T obj : objs) {
      String text = getText.apply(obj);
      if (text != null && SolrConstants.HL_REGEX.matcher(text).find()) {
        cnt++;
      }
    }
    return cnt;
  }

  Function<Description, String> getDescriptions = new Function<Description, String>() {
    @Nullable
    @Override
    public String apply(@Nullable Description input) {
      return input.getDescription();
    }
  };

  Function<VernacularName, String> getVernacular = new Function<VernacularName, String>() {
    @Nullable
    @Override
    public String apply(@Nullable VernacularName input) {
      return input.getVernacularName();
    }
  };

  @Test
  public void testVernacularNames() {
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> resp =
      search("Sciurus vulgaris Linnaeus", NameUsageSearchParameter.RANK, Rank.SPECIES);
    assertEquals(1, resp.getResults().size());
    NameUsageSearchResult nu = resp.getResults().get(0);
    assertEquals(2, nu.getVernacularNames().size());

    resp = search("Sciurus vulgaris Linnaeus", NameUsageSearchParameter.RANK, Rank.SPECIES);
    assertEquals(2, resp.getResults().get(0).getVernacularNames().size());

    resp = search("Sciurus vulgaris Linnaeus", NameUsageSearchParameter.RANK, Rank.SPECIES);
    assertEquals(2, resp.getResults().get(0).getVernacularNames().size());

    resp = search("Eichhörnchen", null, null);
    assertEquals(2, resp.getResults().size());

    resp = search("Europäisches Eichhörnchen", null, null);
    assertEquals(1, resp.getResults().size());
    assertEquals(2, resp.getResults().get(0).getVernacularNames().size());
    Set<String> vnames = getVernacularNamesSet(resp.getResults().get(0));
    assertTrue(vnames.contains("Europäisches Eichhörnchen"));
  }

  @Test
  public void testExtendedSearch() {
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> resp = search("Sciurus vulgaris Linnaeus", NameUsageSearchParameter.RANK, Rank.SPECIES);
    assertEquals(1, resp.getResults().size());
    NameUsageSearchResult nu = resp.getResults().get(0);
    assertEquals(2, nu.getVernacularNames().size());

    resp = search("Sciurus vulgaris Linnaeus", NameUsageSearchParameter.RANK, Rank.SPECIES);
    assertEquals(2, resp.getResults().get(0).getVernacularNames().size());

    resp = search("Sciurus vulgaris Linnaeus", NameUsageSearchParameter.RANK, Rank.SPECIES);
    assertEquals(2, resp.getResults().get(0).getVernacularNames().size());

    resp = search("Eichhörnchen", null, null);
    assertEquals(2, resp.getResults().size());

    resp = search("Europäisches Eichhörnchen", null, null);
    assertEquals(1, resp.getResults().size());
    assertEquals(2, resp.getResults().get(0).getVernacularNames().size());
    Set<String> vnames = getVernacularNamesSet(resp.getResults().get(0));
    assertTrue(vnames.contains("Europäisches Eichhörnchen"));

    // test good query with a rank enum name
    NameUsageSearchRequest req = new NameUsageSearchRequest(0L, 50);
    req.setQ("Rodent");
    req.getQueryFields().clear();
    assertSearch(req, 0l, null);

    req.getQueryFields().add(NameUsageSearchRequest.QueryField.DESCRIPTION);
    assertSearch(req, 2l, null);

    req.setQ("Palearctic");
    assertSearch(req, 1l, null);

    req.getQueryFields().clear();
    req.getQueryFields().add(NameUsageSearchRequest.QueryField.VERNACULAR);
    assertSearch(req, 0l, null);
  }

  private void assertSearch(String q, Long expectedCount) {
    assertSearch(q, null, null, expectedCount, null);
  }

  private SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> assertSearch(String q, NameUsageSearchParameter facet, Enum<?> facetFilter, Long expectedCount, Integer expectedFacetCounts) {
    return assertSearch(buildSearch(q, facet, facetFilter), expectedCount, expectedFacetCounts);
  }

  private SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> assertSearch(NameUsageSearchRequest req, Long expectedCount, Integer expectedFacetCounts) {

    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response = searchService.search(req);

    // assert
    if (expectedCount != null) {
      assertEquals(expectedCount, response.getCount());
    }

    if (expectedFacetCounts != null) {
      assertEquals(1, response.getFacets().size());
      assertEquals(expectedFacetCounts, (Integer) response.getFacets().get(0).getCounts().size());
    }
    if (req.getFacets().isEmpty()) {
      Assert.assertTrue(response.getFacets().isEmpty());
    }

    return response;
  }

  private Set<String> getVernacularNamesSet(NameUsageSearchResult usage) {
    Set<String> vnames = Sets.newHashSet();
    for (VernacularName vn : usage.getVernacularNames()) {
      vnames.add(vn.getVernacularName());
    }
    return vnames;
  }

  private SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> search(String q, NameUsageSearchParameter facet, Enum<?> filter) {
    return searchService.search(buildSearch(q, facet, filter));
  }

  private NameUsageSearchRequest buildSearch(String q, NameUsageSearchParameter facet, Enum<?> filter) {

    // build request
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(0L, 10);
    searchRequest.setQ(q);
    if (filter != null) {
      searchRequest.addParameter(facet, filter);
      searchRequest.addParameter(facet, filter);
    }
    if (facet != null) {
      searchRequest.addFacets(facet);
    }

    // query
    return searchRequest;
  }

  /**
   * Utility method for testing suggest service.
   */
  private List<NameUsageSuggestResult> searchSuggest(String q) {
    NameUsageSuggestRequest req = new NameUsageSuggestRequest();
    req.setQ(q);
    req.setLimit(100);
    req.addParameter(NameUsageSearchParameter.DATASET_KEY, SQUIRRELS_DATASET_KEY);
    return searchService.suggest(req);
  }
}
