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
package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.common.search.SearchResponse;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_LIMIT;
import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_OFFSET;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
public class NameUsageSearchWsClientTest {

  @Mock
  private WebResource resource;
  private NameUsageSearchWsClient client;


  @Before
  public void setUp() {
    client = new NameUsageSearchWsClient(resource);
  }

  @Test
  @PrepareForTest(WebResource.Builder.class)
  public void testList() {
    WebResource.Builder builder = PowerMockito.mock(WebResource.Builder.class);
    NameUsageSearchRequest searchRequest = new NameUsageSearchRequest(DEFAULT_PARAM_OFFSET, DEFAULT_PARAM_LIMIT);
    searchRequest.setQ("*");
    when(resource.type(any(String.class))).thenReturn(builder);
    when(resource.accept(any(String.class))).thenReturn(builder);
    when(resource.path(any(String.class))).thenReturn(resource);
    when(resource.queryParam(Matchers.<String>any(), Matchers.<String>any())).thenReturn(resource);
    when(resource.queryParams(any(MultivaluedMap.class))).thenReturn(resource);
    // there is no path, it uses the default URL /checklist
    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> defaultResponse =
      new SearchResponse<NameUsageSearchResult, NameUsageSearchParameter>(DEFAULT_PARAM_OFFSET, DEFAULT_PARAM_LIMIT);
    when(builder.get(any(GenericType.class))).thenReturn(defaultResponse);
    when(resource.get(any(GenericType.class))).thenReturn(defaultResponse);

    SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> response = client.search(searchRequest);
    Assert.assertNotNull(response);
  }
}
