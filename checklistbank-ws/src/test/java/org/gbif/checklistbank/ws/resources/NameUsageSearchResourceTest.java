package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.NameUsageSearchService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test class for the NameUsageSearchResource class
 */
public class NameUsageSearchResourceTest {

  private NameUsageSearchService mockService;
  private NameUsageSearchResource resource;
  private SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> searchResponse;

  @Before
  public void setUp() throws Exception {
    mockService = mock(NameUsageSearchService.class);
    resource = new NameUsageSearchResource(mockService);

    searchResponse = new SearchResponse<NameUsageSearchResult, NameUsageSearchParameter>(0, 20);
    searchResponse.setCount(1000L);

  }

  @Test
  public void testSearch() throws Exception {
    when(mockService.search(Matchers.any(NameUsageSearchRequest.class))).thenReturn(searchResponse);
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> searchResponse2 = resource.search(new NameUsageSearchRequest(1000L, 20));
    assertNotNull(searchResponse2);
  }
}
