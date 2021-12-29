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
package org.gbif.checklistbank.index.service;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestResult;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.common.search.SearchException;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Checklist Bank Search interface implementation using Solr/lucene.
 * This class uses a remote to interact with CLB index.
 */
@Service
public class NameUsageSearchServiceImpl implements NameUsageSearchService {
  private static final Logger LOG = LoggerFactory.getLogger(NameUsageSearchServiceImpl.class);
  private static final int DEFAULT_SUGGEST_LIMIT = 10;
  private static final int MAX_SUGGEST_LIMIT = 100;

  /*
 * Solr server instance, this abstract type is used because it can hold instance of:
 * CommonsHttpSolrServer or EmbeddedSolrServer.
 */
  private final SolrClient solrClient;
  private final ResponseBuilder responseBuilder = new ResponseBuilder();
  private final SolrQueryBuilder queryBuilder = new SolrQueryBuilder();

  /**
   * Default constructor.
   */
  @Autowired
  public NameUsageSearchServiceImpl(SolrClient solrClient) {
    this.solrClient = solrClient;
  }

  /**
   * Issues a SolrQuery and converts the response to a SearchResponse object. Besides, the facets and paging
   * parameter and responses are handled in the request and response objects.
   *
   * @param request the request that contains the search parameters
   * @return the SearchResponse of the search operation
   */
  @Override
  public SearchResponse<NameUsageSearchResult, NameUsageSearchParameter> search(NameUsageSearchRequest request) {
    SolrQuery solrQuery = queryBuilder.build(request);
    QueryResponse response = query(solrQuery);
    return responseBuilder.buildSearch(request, response);
  }

  @Override
  public List<NameUsageSuggestResult> suggest(NameUsageSuggestRequest request) {
    // add defaults
    if (!request.getParameters().containsKey(NameUsageSearchParameter.DATASET_KEY)) {
      // if the datasetKey parameters is not in the list, the GBIF nub is used by default
      request.addParameter(NameUsageSearchParameter.DATASET_KEY, Constants.NUB_DATASET_KEY.toString());
    }
    if (request.getLimit() < 1 || request.getLimit() > MAX_SUGGEST_LIMIT) {
      LOG.info("Suggest request with limit {} found. Reset to default {}", request.getLimit(), DEFAULT_SUGGEST_LIMIT);
      request.setLimit(DEFAULT_SUGGEST_LIMIT);
    }
    if (request.getOffset() > 0) {
      LOG.debug("Suggest request with offset {} found", request.getOffset());
    }

    // execute
    SolrQuery solrQuery = queryBuilder.build(request);
    QueryResponse response = query(solrQuery);
    return responseBuilder.buildSuggest(response);
  }

  private QueryResponse query(SolrQuery query) {
    try {
      // Executes the search operation in Solr
      LOG.debug("Solr query executed: {}", query);
      return solrClient.query(query);

    } catch (SolrServerException e) {
      if (e.getRootCause() instanceof IllegalArgumentException) {
        LOG.error("Bad query", e);
        throw (IllegalArgumentException) e.getRootCause();
      } else {
        LOG.error("Error querying solr {}", query, e);
        throw new SearchException(e);
      }

    } catch (IOException e) {
      LOG.error("Error querying solr {}", query, e);
      throw new SearchException(e);
    }
  }

}
