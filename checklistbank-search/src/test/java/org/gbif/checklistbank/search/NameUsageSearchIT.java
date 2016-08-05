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
package org.gbif.checklistbank.search;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestResult;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.index.backfill.SolrBackfillBaseIT;
import org.gbif.common.search.util.SolrConstants;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests using an embedded solr server with the mybatis squirrels test dataset.
 * The solr index will be rebuild before the test using the NameUsageIndexerBaseIT base class.
 */
public class NameUsageSearchIT extends SolrBackfillBaseIT {

  protected final Logger log = LoggerFactory.getLogger(NameUsageSearchIT.class);
  private static final String PROPERTY_FILE = "checklistbank.properties";
  private static NameUsageSearchService searchService;
  private static String SQUIRRELS_DATASET_KEY = "109aea14-c252-4a85-96e2-f5f4d5d088f4";
  private final Function<NameUsageSearchResult, Integer> extractKeyFunction =
    new Function<NameUsageSearchResult, Integer>() {

      public Integer apply(NameUsageSearchResult n) {
        return n == null ? null : n.getKey();
      }
    };

  @BeforeClass
  public static void setup() throws IOException, SAXException, ParserConfigurationException {
    // creates squirrels db and solr index & server using its own injector
    SolrBackfillBaseIT.setup();

    // insert new injector for this test, reusing existing solr server
    Properties props = PropertiesUtil.loadProperties(PROPERTY_FILE);
    Injector injector = Guice.createInjector(new SearchTestModule(props, solr()));

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
  public void testCanonicalHighlighting() {
    // build request
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(0L, 20);
    searchRequest.setQ("vulgaris");
    searchRequest.setHighlight(true);

    // query
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response = searchService.search(searchRequest);

    // assert
    Assert.assertEquals((Long) 10l, response.getCount());
    // this usage also matches description and should therefore come first
    Collection<Integer> keys = Collections2.transform(response.getResults(), extractKeyFunction);
    assertEquals(10, keys.size());
    assertThat(keys,
      hasItems(100000025, 100000030, 100000031, 100000032, 100000033, 100000034, 100000035, 100000036, 100000037
        , 100000038));

    int highlighted = 0;
    for (NameUsageSearchResult nu : response.getResults()) {
      System.out.println(nu.getCanonicalName());

      if (SolrConstants.HL_REGEX.matcher(nu.getCanonicalName()).find()) {
        highlighted++;
      }
    }
    Assert.assertEquals("wrong number of canonical highlights", 10, highlighted);
  }

  @Test
  public void testDescriptionHighlighting() {
    // build request
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(0L, 10);
    searchRequest.setQ("Paleocene");
    searchRequest.setHighlight(true);

    // query
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response = searchService.search(searchRequest);

    // assert
    Assert.assertEquals((Long) 2l, response.getCount());
    Assert.assertEquals((Integer) 100000004, response.getResults().get(0).getKey());
    List<Description> descriptions = response.getResults().get(0).getDescriptions();
    Assert.assertEquals(4, descriptions.size());

    int highlighted = 0;
    for (Description d : descriptions) {
      if (SolrConstants.HL_REGEX.matcher(d.getDescription()).find()) {
        highlighted++;
      }
    }
    Assert.assertEquals(1, highlighted);
  }

  @Test
  public void testEnumFilterRequest() {
    // test good query with a rank enum name
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(0L, 10);
    searchRequest.addParameter(NameUsageSearchParameter.RANK, "genus");
    searchService.search(searchRequest);
  }

  @Test
  public void testSearchScientificNameNoFacets() {
    assertSearch("Rodentia", 42L);
    assertSearch("Rodentia Bowdich 1821", 2L);
    assertSearch("Rodentia Bowdich,", 2L);
    assertSearch("Puma concolor, 1821", 0L);
  }

  @Test
  public void testSearchScientificNameWithRankFacet() {
    assertSearch("vulgaris", NameUsageSearchParameter.RANK, null, 10L, null);
    assertSearch("Sciurus vulgaris", NameUsageSearchParameter.RANK, null, 10L, null);
    assertSearch("Sciurus vulgaris", NameUsageSearchParameter.RANK, Rank.SPECIES, 1l, null);
    assertSearch("Sciurus vulgaris", NameUsageSearchParameter.RANK, Rank.VARIETY, 2l, null);
  }

  @Test
  public void testNomStatusFacet() {
    assertSearch(null, NameUsageSearchParameter.NOMENCLATURAL_STATUS, NomenclaturalStatus.NUDUM, 1L, 2);
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> resp =
      search(null, NameUsageSearchParameter.NOMENCLATURAL_STATUS, NomenclaturalStatus.NUDUM);
    assertEquals((Integer) 100000026, resp.getResults().get(0).getKey());
  }

  @Test
  public void testSuggest() {
    log.info("running test");
    List<NameUsageSuggestResult> results = searchSuggest("sci");
    log.error(results != null ? "RESULTS NOT IS NULL" : "is null");
    Assert.assertTrue(results != null && !results.isEmpty());
  }

  @Test
  public void testVernacularHighlighting() {
    // build request
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(0L, 10);
    searchRequest.setQ("Eichhörnchen");
    searchRequest.setHighlight(true);

    // query
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response = searchService.search(searchRequest);

    // assert
    Assert.assertEquals((Long) 2l, response.getCount());
    // this usage also matches description and should therefore come first
    Assert.assertEquals((Integer) 100000040, response.getResults().get(0).getKey());
    List<VernacularName> names = response.getResults().get(0).getVernacularNames();
    Assert.assertEquals(2, names.size());

    int highlighted = 0;
    for (VernacularName d : names) {
      if (SolrConstants.HL_REGEX.matcher(d.getVernacularName()).find()) {
        highlighted++;
      }
    }
    Assert.assertEquals(1, highlighted);
  }

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

  private void assertSearch(String q, Long expectedCount) {
    assertSearch(q, null, null, expectedCount, null);
  }

  private SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> assertSearch(String q,
    NameUsageSearchParameter facet,
    Enum<?> facetFilter, Long expectedCount, Integer expectedFacetCounts) {

    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response = search(q, facet, facetFilter);

    // assert
    if (expectedCount != null) {
      assertEquals(expectedCount, response.getCount());
    }

    if (facet != null && expectedFacetCounts != null) {
      assertEquals(1, response.getFacets().size());
      assertEquals(expectedFacetCounts, (Integer) response.getFacets().get(0).getCounts().size());
    }
    if (facet == null) {
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

  private SearchResponse<NameUsageSearchResult, NameUsageSearchParameter>
    search(String q, NameUsageSearchParameter facet, Enum<?> filter) {

    // build request
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(0L, 10);
    searchRequest.setQ(q);
    if (filter != null) {
      searchRequest.addParameter(facet, filter);
    }
    if (facet != null) {
      searchRequest.addFacets(facet);
    }

    // query
    return searchService.search(searchRequest);
  }

  /**
   * Utility method for testing suggest service.
   */
  private List<NameUsageSuggestResult> searchSuggest(String q) {
    NameUsageSuggestRequest req = new NameUsageSuggestRequest();
    req.setQ(q);
    req.setLimit(10);
    req.addParameter(NameUsageSearchParameter.DATASET_KEY, SQUIRRELS_DATASET_KEY);
    return searchService.suggest(req);
  }
}
