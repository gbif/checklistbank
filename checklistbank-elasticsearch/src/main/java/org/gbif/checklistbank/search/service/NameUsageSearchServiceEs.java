/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.search.service;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.search.*;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.checklistbank.index.model.NameUsageAvro;
import org.gbif.common.search.EsSearchRequestBuilder;

import java.util.List;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Qualifier("nameUsageSearchServiceEs")
public class NameUsageSearchServiceEs implements NameUsageSearchService {

  private static final int DEFAULT_SUGGEST_LIMIT = 10;
  private static final int MAX_SUGGEST_LIMIT = 100;

  private final NameUsageEsResponseParser searchResponseParser = NameUsageEsResponseParser.create();

  private final NameUsageSuggestEsResponseParser suggestResponseParser = NameUsageSuggestEsResponseParser.create();

  private final ElasticsearchClient elasticsearchClient;
  private final String index;

  private final EsSearchRequestBuilder<NameUsageSearchParameter> searchRequestBuilder =
      new EsSearchRequestBuilder<>(new NameUsageEsFieldMapper());

  private final EsSearchRequestBuilder<NameUsageSearchParameter> suggestRequestBuilder =
    new EsSearchRequestBuilder<>(new NameUsageSuggestEsFieldMapper());

  public NameUsageSearchServiceEs(String index, ElasticsearchClient elasticsearchClient) {
    this.index = index;
    this.elasticsearchClient = elasticsearchClient;
  }

  @Override
  @SneakyThrows
  public SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> search(
      NameUsageSearchRequest nameUsageSearchRequest) {
      SearchRequest searchRequest =
          searchRequestBuilder.buildFacetedSearchRequest(nameUsageSearchRequest,    true, index);

      return searchResponseParser.buildSearchResponse(
        elasticsearchClient.search(searchRequest, NameUsageAvro.class), nameUsageSearchRequest);
  }

  @Override
  @SneakyThrows
  public List<NameUsageSuggestResult> suggest(NameUsageSuggestRequest request) {
    // add defaults
    if (!request.getParameters().containsKey(NameUsageSearchParameter.DATASET_KEY)) {
      // if the datasetKey parameters is not in the list, the GBIF nub is used by default
      request.addParameter(NameUsageSearchParameter.DATASET_KEY, Constants.NUB_DATASET_KEY.toString());
    }
    if (request.getLimit() < 1 || request.getLimit() > MAX_SUGGEST_LIMIT) {
      log.info("Suggest request with limit {} found. Reset to default {}", request.getLimit(), DEFAULT_SUGGEST_LIMIT);
      request.setLimit(DEFAULT_SUGGEST_LIMIT);
    }
    if (request.getOffset() > 0) {
      log.debug("Suggest request with offset {} found", request.getOffset());
    }
    SearchRequest searchRequest = suggestRequestBuilder.buildSearchRequest(request, index);
    return suggestResponseParser.buildSearchResponse(elasticsearchClient.search(searchRequest, NameUsageAvro.class), request).getResults();
  }
}
