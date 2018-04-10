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

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.FacetParams;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.common.search.solr.QueryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.gbif.checklistbank.index.service.SolrMapping.FACET_MAPPING;
import static org.gbif.common.search.solr.QueryUtils.*;
import static org.gbif.common.search.solr.QueryUtils.NOT_OP;
import static org.gbif.common.search.solr.SolrConstants.*;
import static org.gbif.ws.util.WebserviceParameter.DEFAULT_SEARCH_PARAM_VALUE;

/**
 * Builder class to generate solr queries based on the dismax query parser.
 */
public class SolrQueryBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(SolrQueryBuilder.class);

  private static final String QUERY_PARSER = "dismax";
  private static final Map<NameUsageSearchRequest.QueryField, String> QUERY_FIELDS = ImmutableMap.of(
      NameUsageSearchRequest.QueryField.DESCRIPTION, "description^0.1",
      NameUsageSearchRequest.QueryField.VERNACULAR, "vernacular_name^3",
      NameUsageSearchRequest.QueryField.SCIENTIFIC, "canonical_name^10 scientific_name^2 species subgenus family"
  );
  private static final Map<NameUsageSearchRequest.QueryField, String> PHRASE_FIELDS = ImmutableMap.of(
      NameUsageSearchRequest.QueryField.DESCRIPTION, "description^2",
      NameUsageSearchRequest.QueryField.VERNACULAR, "vernacular_name^20",
      NameUsageSearchRequest.QueryField.SCIENTIFIC, "scientific_name^100 canonical_name^50"
  );
  // boost accepted taxa and scientific names
  private static final String BOOST_QUERY = "taxonomic_status_key:0^1.5 name_type:0^1.5";
  private static final String BOOST_FUNCTION = "product(2,sub(" + Rank.values().length + ",rank_key))";
  private static final String SUGGEST_QUERY_FIELDS = "canonical_name_tokenized^10 canonical_name_ngram^5 canonical_name_ngram_tokenized^2 scientific_name";
  private static final String SUGGEST_PHRASE_FIELDS = "canonical_name^10";
  private static final SolrQuery.SortClause DEFAULT_SORT = new SolrQuery.SortClause("key", SolrQuery.ORDER.asc);

  private static final Integer FRAGMENT_SIZE = 100;


  private static String prepareQ(String q) {
    if (Strings.isNullOrEmpty(q)) return null;
    q = q.trim();
    // the common-ws utils replaces empty queries with * as the default - this does not work for dismax, remove it
    if (q.equals(DEFAULT_SEARCH_PARAM_VALUE)) return null;

    return q.contains(BLANK) ? toPhraseQuery(q) : q;
  }

  private static String buildFields(Map<NameUsageSearchRequest.QueryField, String> config, Set<NameUsageSearchRequest.QueryField> fields) {
    StringBuilder sb = new StringBuilder();
    for (NameUsageSearchRequest.QueryField f : fields) {
      sb.append(config.get(f));
      sb.append(" ");
    }
    return sb.toString().trim();
  }

  public SolrQuery build(NameUsageSearchRequest request) {
    SolrQuery query = buildBase(request);

    // dismax fields
    query.set(DisMaxParams.QF, buildFields(QUERY_FIELDS, request.getQueryFields()));
    query.set(DisMaxParams.PF, buildFields(PHRASE_FIELDS, request.getQueryFields()));

    // request facets
    requestFacets(request, query);

    // highlight
    setHighLightParams(request, query);

    LOG.debug("Solr search query build: {}", query);
    return query;
  }

  public SolrQuery build(NameUsageSuggestRequest request) {
    SolrQuery query = buildBase(request);

    // dismax fields
    query.set(DisMaxParams.QF, SUGGEST_QUERY_FIELDS);
    query.set(DisMaxParams.PF, SUGGEST_PHRASE_FIELDS);

    LOG.debug("Solr suggest query build: {}", query);
    return query;
  }

  private SolrQuery buildBase(SearchRequest<NameUsageSearchParameter> request) {
    SolrQuery query = new SolrQuery();
    // q param
    String q = prepareQ(request.getQ());
    if (!Strings.isNullOrEmpty(q)) {
      query.setQuery(q);
    } else {
      // otherwise use stable sorting by key (with q we sort by relevance)
      query.addSort(DEFAULT_SORT);
    }
    // use dismax query parser
    query.set("defType", QUERY_PARSER);
    // sets the default catch all, alternative query if q above is empty
    query.set(DisMaxParams.ALTQ, DEFAULT_QUERY);

    // facet based filter query
    setFacetFilterQuery(request, query);

    // boost accepted status
    query.set(DisMaxParams.BQ, BOOST_QUERY);
    // boost higher ranks
    query.set(DisMaxParams.BF, BOOST_FUNCTION);

    // paging
    QueryUtils.setQueryPaging(request, query);

    return query;
  }

  /**
   * Helper method that sets the highlighting parameters.
   *
   * @param searchRequest the searchRequest used to extract the parameters
   * @param solrQuery     this object is modified by adding the facets parameters
   */
  private void setHighLightParams(NameUsageSearchRequest searchRequest, SolrQuery solrQuery) {
    if (searchRequest.isHighlight()) {
      solrQuery.setHighlight(searchRequest.isHighlight());
      solrQuery.setHighlightSnippets(NUM_HL_SNIPPETS);
      solrQuery.setHighlightFragsize(FRAGMENT_SIZE);
      for (NameUsageSearchRequest.QueryField hlField : searchRequest.getHighlightFields()) {
        solrQuery.addHighlightField(SolrMapping.HIGHLIGHT_FIELDS.get(hlField));
      }
    }
  }

  /**
   * Adds the filter query to SolrQuery object.
   * Creates a conjunction of disjunctions: disjunctions(ORs) are created for the filter applied to the same field;
   * those disjunctions are joint in a big conjunction.
   */
  private static void setFacetFilterQuery(SearchRequest<NameUsageSearchParameter> request, SolrQuery solrQuery) {
    Multimap<NameUsageSearchParameter, String> params = request.getParameters();
    if (params != null) {
      for (NameUsageSearchParameter param : params.keySet()) {
        String solrField = FACET_MAPPING.get(param);
        if (solrField != null) {
          List<String> predicates = Lists.newArrayList();
          Boolean negated = null;
          for (String value : params.get(param)) {
            if (Strings.isNullOrEmpty(value)) {
              throw new IllegalArgumentException("Null value not allowed for filter parameter " + param);
            }

            // treat negation
            if (negated == null) {
              negated = QueryUtils.isNegated(value);
            } else {
              // make sure we do not mix negated and unnegated filters for the same parameter - this is too complex and not supported
              if (QueryUtils.isNegated(value) != negated) {
                throw new IllegalArgumentException("Mixing of negated and not negated filters for the same parameter " + param.name() + " is not allowed");
              }
            }

            // strip off negation symbol before we parse the value
            if (negated) {
              value = QueryUtils.removeNegation(value);
            }

            // parse value into typed instance
            String filterVal;
            if (Enum.class.isAssignableFrom(param.type())) {
              Enum<?> e = VocabularyUtils.lookupEnum(value, (Class<? extends Enum<?>>) param.type());
              filterVal = String.valueOf(e.ordinal());

            } else if (UUID.class.isAssignableFrom(param.type())) {
              filterVal = UUID.fromString(value).toString();

            } else if (Double.class.isAssignableFrom(param.type())) {
              filterVal = String.valueOf(Double.parseDouble(value));

            } else if (Integer.class.isAssignableFrom(param.type())) {
              filterVal = String.valueOf(Integer.parseInt(value));

            } else if (Boolean.class.isAssignableFrom(param.type())) {
              filterVal = String.valueOf(Boolean.parseBoolean(value));

            } else {
              filterVal = toPhraseQuery(value);
            }

            final String predicate = PARAMS_JOINER.join(solrField, filterVal);
            predicates.add(predicate);
          }

          // combine all parameter predicates with OR
          if (!predicates.isEmpty()) {
            String parenthesis = toParenthesesQuery(PARAMS_OR_JOINER.join(predicates));
            // tag filter queries so we can exclude them later for multi value faceting
            // http://yonik.com/multi-select-faceting/
            solrQuery.addFilterQuery(tag(solrField, negated ? NOT_OP + parenthesis : parenthesis));
          }
        }
      }
    }
  }

  /**
   * Helper method that sets the parameter for a faceted query.
   *
   * @param searchRequest the searchRequest used to extract the parameters
   * @param solrQuery     this object is modified by adding the facets parameters
   */
  private void requestFacets(FacetedSearchRequest<NameUsageSearchParameter> searchRequest, SolrQuery solrQuery) {

    if (!searchRequest.getFacets().isEmpty()) {
      // Only show facets that contains at least 1 record
      solrQuery.setFacet(true);
      // defaults if not overridden on per field basis
      solrQuery.setFacetMinCount(MoreObjects.firstNonNull(searchRequest.getFacetMinCount(), DEFAULT_FACET_COUNT));
      solrQuery.setFacetMissing(false);
      solrQuery.setFacetSort(DEFAULT_FACET_SORT.toString().toLowerCase());

      if (searchRequest.getFacetLimit() != null) {
        solrQuery.setFacetLimit(searchRequest.getFacetLimit());
      }

      if (searchRequest.getFacetOffset() != null) {
        solrQuery.setParam(FacetParams.FACET_OFFSET, searchRequest.getFacetOffset().toString());
      }

      for (final NameUsageSearchParameter facet : searchRequest.getFacets()) {
        if (!FACET_MAPPING.containsKey(facet)) {
          LOG.warn("{} is no valid facet. Ignore", facet);
          continue;
        }
        final String field = FACET_MAPPING.get(facet);
        if (searchRequest.isMultiSelectFacets()) {
          // use exclusion filter with same name as used in filter query
          // http://wiki.apache.org/solr/SimpleFacetParameters#Tagging_and_excluding_Filters
          // http://yonik.com/multi-select-faceting/
          solrQuery.addFacetField(ex(field, field));
        } else {
          solrQuery.addFacetField(field);
        }

        Pageable facetPage = searchRequest.getFacetPage(facet);
        if (facetPage != null) {
          solrQuery.setParam(perFieldParamName(field, FacetParams.FACET_OFFSET), Long.toString(facetPage.getOffset()));
          solrQuery.setParam(perFieldParamName(field, FacetParams.FACET_LIMIT), Integer.toString(facetPage.getLimit()));
        }
      }
    }
  }

  private static String ex(String tag, String filter) {
    return "{!ex=" + tag + "}" + filter;
  }

  private static String tag(String tag, String filter) {
    return "{!tag=" + tag + "}" + filter;
  }
}
