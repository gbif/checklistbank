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

import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestResult;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.common.search.EsSearchRequestBuilder;

import java.util.List;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Qualifier("nameUsageSearchServiceEs")
public class NameUsageSearchServiceEs implements NameUsageSearchService {

  private static final int DEFAULT_SUGGEST_LIMIT = 10;
  private static final int MAX_SUGGEST_LIMIT = 100;

  private final NameUsageEsResponseParser esResponseParser = NameUsageEsResponseParser.create();
  private final RestHighLevelClient restHighLevelClient;
  private final String index;

  private final EsSearchRequestBuilder<NameUsageSearchParameter> esSearchRequestBuilder =
      new EsSearchRequestBuilder<>(new NameUsageEsFieldMapper(), true);

  @Autowired
  public NameUsageSearchServiceEs(
      @Value("${elasticsearch.registry.index}") String index,
      RestHighLevelClient restHighLevelClient) {
    this.index = index;
    this.restHighLevelClient = restHighLevelClient;
  }

  @Override
  @SneakyThrows
  public SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> search(
      NameUsageSearchRequest nameUsageSearchRequest) {
      SearchRequest searchRequest =
          esSearchRequestBuilder.buildSearchRequest(nameUsageSearchRequest, true, index);
      return esResponseParser.buildSearchResponse(
          restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT), nameUsageSearchRequest);
  }

  @Override
  public List<NameUsageSuggestResult> suggest(NameUsageSuggestRequest request) {
    return null;
  }
}
