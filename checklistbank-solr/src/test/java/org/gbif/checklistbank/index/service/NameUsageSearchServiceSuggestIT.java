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

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestResult;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.checklistbank.index.backfill.SolrTestSetup;
import org.gbif.checklistbank.index.guice.SearchTestModule;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.List;
import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests using an embedded solr server with the mybatis squirrels test dataset.
 * The solr index will be rebuild before the test using the NameUsageIndexerBaseIT base class.
 */
public class NameUsageSearchServiceSuggestIT {

  protected final Logger log = LoggerFactory.getLogger(NameUsageSearchServiceSuggestIT.class);
  private static final String PROPERTY_FILE = "checklistbank.properties";
  private static NameUsageSearchService searchService;
  private static String SQUIRRELS_DATASET_KEY = "109aea14-c252-4a85-96e2-f5f4d5d088f4";

  @BeforeClass
  public static void setup() throws Exception {
    // creates squirrels db and solr index & server using its own injector
    SolrTestSetup setup = new SolrTestSetup(ClbDbTestRule.puma());
    setup.setup();

    // insert new injector for this test, reusing existing solr server
    Properties props = PropertiesUtil.loadProperties(PROPERTY_FILE);
    Injector injector = Guice.createInjector(new SearchTestModule(props, setup.solr()));

    searchService = injector.getInstance(NameUsageSearchService.class);
  }


  @Test
  public void testSuggest() {
    List<NameUsageSuggestResult> results = searchSuggest("Puma");
    // highest rank first
    assertEquals("Puma Jardine, 1834", results.get(0).getScientificName());

    // https://github.com/gbif/checklistbank/issues/11
    assertPumaConcolor("Puma concolor");
    assertPumaConcolor("Puma concolo");
    assertPumaConcolor("Puma concol");
    assertPumaConcolor("Puma conco");
    assertPumaConcolor("Puma conc");
    assertPumaConcolor("Puma con");
    assertPumaConcolor("Puma co");
    assertPumaConcolor("Puma c");

    assertPumaConcolor("concolor");
  }

  private void assertPumaConcolor(String q) {
    List<NameUsageSuggestResult> results = searchSuggest(q);
    assertEquals(2435099, (int) results.get(0).getKey());
    assertEquals("Puma concolor (Linnaeus, 1771)", results.get(0).getScientificName());

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
    req.setLimit(250);
    req.addParameter(NameUsageSearchParameter.DATASET_KEY, Constants.NUB_DATASET_KEY.toString());
    return searchService.suggest(req);
  }
}
