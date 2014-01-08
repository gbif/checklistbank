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
package org.gbif.checklistbank.search.ws.resources;

import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestRequest;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.ws.util.ExtraMediaTypes;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
@Path("/")
public class NameUsageSearchResource {

  private static final Logger LOG = LoggerFactory.getLogger(NameUsageSearchResource.class);

  private final NameUsageSearchService searchService;

  @Inject
  public NameUsageSearchResource(NameUsageSearchService searchService) {
    this.searchService = searchService;
  }

  @GET
  @Path("search")
  public SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> search(@Context NameUsageSearchRequest searchRequest) {
    LOG.debug("Search operation received {} ", searchRequest);
    return searchService.search(searchRequest);
  }

  @Path("suggest")
  @GET
  public List<NameUsageSearchResult> suggest(@Context NameUsageSuggestRequest searchSuggestRequest) {
    LOG.debug("Suggest operation received {} ", searchSuggestRequest);
    return searchService.suggest(searchSuggestRequest);
  }

}
