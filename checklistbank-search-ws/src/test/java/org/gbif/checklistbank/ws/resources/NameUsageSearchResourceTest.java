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
package org.gbif.checklistbank.ws.resources;

import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.checklistbank.search.ws.resources.NameUsageSearchResource;

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
