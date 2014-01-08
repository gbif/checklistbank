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
package org.gbif.checklistbank.search.service;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestRequest;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.api.vocabulary.NameType;
import org.gbif.checklistbank.index.model.NameUsageSolrSearchResult;
import org.gbif.common.search.service.SolrSearchSuggestService;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;

import static org.gbif.common.search.util.SearchConstants.NOT_OP;


/**
 * Checklist Bank Search interface implementation using Solr/lucene.
 * This class uses a remote to interact with CLB index.
 */
public class NameUsageSearchServiceImpl
  extends SolrSearchSuggestService<NameUsageSearchResult, NameUsageSearchParameter, NameUsageSolrSearchResult,
  NameUsageSearchRequest, NameUsageSuggestRequest> implements NameUsageSearchService {

  // Default primary sort order
  private static final Map<String, SolrQuery.ORDER> PRIMARY_SORT_ORDER =
    new ImmutableMap.Builder<String, SolrQuery.ORDER>()
      .put("score", SolrQuery.ORDER.desc)
      .put("num_descendants", SolrQuery.ORDER.desc)
      .put("scientific_name", SolrQuery.ORDER.asc).build();

  // Default sort order for suggest results
  private static final Map<String, SolrQuery.ORDER> DEFAULT_SUGGEST_ORDER =
    new ImmutableMap.Builder<String, SolrQuery.ORDER>()
      .put("score", SolrQuery.ORDER.desc)
      .put("rank_key", SolrQuery.ORDER.asc).build();


  private static final Multimap<NameUsageSearchParameter, String> SUGGEST_DEFAULT_FILTERS = ArrayListMultimap.create();

  protected static final int DEFAULT_LIMIT = 10;

  static {
    // Suggest default filters for suggest service: names of type INFORMAL and BLACKLISTED are excluded by default.
    SUGGEST_DEFAULT_FILTERS.put(NameUsageSearchParameter.NAME_TYPE, NOT_OP + NameType.INFORMAL.name());
    SUGGEST_DEFAULT_FILTERS.put(NameUsageSearchParameter.NAME_TYPE, NOT_OP + NameType.BLACKLISTED.name());
  }

  /**
   * Default constructor.
   */
  @Inject
  public NameUsageSearchServiceImpl(SolrServer server) {
    // Type parameter bounded to NameUsageSearch
    super(server, null, NameUsageSearchResult.class, NameUsageSolrSearchResult.class,
      NameUsageSearchParameter.class, PRIMARY_SORT_ORDER, DEFAULT_SUGGEST_ORDER, false);
  }

  @Override
  public List<NameUsageSearchResult> suggest(NameUsageSuggestRequest suggestRequest) {
    if (!suggestRequest.getParameters().containsKey(NameUsageSearchParameter.DATASET_KEY)) {
      // if the datasetKey parameters is not in the list, the GBIF nub is used by default
      suggestRequest.addParameter(NameUsageSearchParameter.DATASET_KEY, Constants.NUB_DATASET_KEY.toString());
      for (NameUsageSearchParameter p : SUGGEST_DEFAULT_FILTERS.keys()) {
        suggestRequest.addParameter(p, SUGGEST_DEFAULT_FILTERS.get(p));
      }
    }
    return super.suggest(suggestRequest);
  }

}
