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
import org.gbif.api.model.checklistbank.search.NameUsageSuggestRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestResult;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankSearchWs;
import org.gbif.ws.client.BaseWsSuggestClient;

import java.util.List;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * Client-side implementation to the NameUsageSearchService.
 */
public class NameUsageSearchWsClient
  extends BaseWsSuggestClient<NameUsageSearchResult, NameUsageSearchParameter,
  NameUsageSearchRequest, NameUsageSuggestResult, NameUsageSuggestRequest> implements NameUsageSearchService {

  private static final GenericType<SearchResponse<NameUsageSearchResult, NameUsageSearchParameter>> SEARCH_TYPE =
    new GenericType<SearchResponse<NameUsageSearchResult, NameUsageSearchParameter>>() {
    };

  private static final GenericType<List<NameUsageSuggestResult>> SUGGEST_TYPE =
    new GenericType<List<NameUsageSuggestResult>>() {
    };

  @Inject
  public NameUsageSearchWsClient(@ChecklistBankSearchWs WebResource resource) {
    super(resource, SEARCH_TYPE, SUGGEST_TYPE);
  }

}
