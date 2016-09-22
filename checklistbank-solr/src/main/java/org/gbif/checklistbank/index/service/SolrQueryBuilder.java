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

import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.checklistbank.search.NameUsageSuggestRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.common.search.model.SuggestMapping;
import org.gbif.common.search.util.QueryUtils;

import java.util.List;
import java.util.UUID;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.FacetParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.checklistbank.index.service.SolrMapping.FACET_MAPPING;
import static org.gbif.common.search.builder.SolrQueryUtils.DEFAULT_FACET_COUNT;
import static org.gbif.common.search.builder.SolrQueryUtils.DEFAULT_FACET_SORT;
import static org.gbif.common.search.builder.SolrQueryUtils.perFieldParamName;
import static org.gbif.common.search.builder.SolrQueryUtils.taggedField;
import static org.gbif.common.search.util.QueryUtils.PARAMS_JOINER;
import static org.gbif.common.search.util.QueryUtils.PARAMS_OR_JOINER;
import static org.gbif.common.search.util.QueryUtils.toParenthesesQuery;
import static org.gbif.common.search.util.QueryUtils.toPhraseQuery;
import static org.gbif.common.search.util.SolrConstants.BLANK;
import static org.gbif.common.search.util.SolrConstants.DEFAULT_QUERY;
import static org.gbif.common.search.util.SolrConstants.FACET_FILTER_EX;
import static org.gbif.common.search.util.SolrConstants.NUM_HL_SNIPPETS;

/**
 * Builder class that helps in the creation process of query patterns for classes annotated with
 * {@link SuggestMapping}.
 */
public class SolrQueryBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(SolrQueryBuilder.class);

  private static final String QUERY_PARSER   = "dismax";
  private static final String QUERY_FIELDS   = "canonical_name^5 scientific_name^3 vernacular_name^2 species subgenus family description^0.1";
  private static final String PHRASE_FIELDS  = "scientific_name^1000 canonical_name^500 vernacular_name^50";
  private static final String BOOST_QUERY    = "taxonomic_status_key:0^2";
  private static final String BOOST_FUNCTION = "sub(" + Rank.values().length + ",rank_key)";
  private static final String SUGGEST_QUERY_FIELDS   = "scientific_name_ngram canonical_name_ngram^2 canonical_name^4 scientific_name^3";
  private static final String SUGGEST_PHRASE_FIELDS  = "canonical_name^100";

  private static final Integer FRAGMENT_SIZE = 100;


  private static String cleanQ(String q) {
    if (Strings.isNullOrEmpty(q)) return null;
    q = q.trim();
    return q.contains(BLANK) ? QueryUtils.toPhraseQuery(q) : q;
  }

  public SolrQuery build(NameUsageSearchRequest request) {
    SolrQuery query = buildBase(request);

    // dismax fields
    query.set(DisMaxParams.QF, QUERY_FIELDS);
    query.set(DisMaxParams.PF, PHRASE_FIELDS);

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
    //Preconditions.checkArgument(request.getOffset() <= maxOffset - request.getLimit(), "maximum offset allowed is %s", this.maxOffset);
    SolrQuery query = new SolrQuery();
    // q param
    query.setQuery(cleanQ(request.getQ()));
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
   * @param solrQuery this object is modified by adding the facets parameters
   */
  private void setHighLightParams(NameUsageSearchRequest searchRequest, SolrQuery solrQuery) {
    if (searchRequest.isHighlight()) {
      solrQuery.setHighlight(searchRequest.isHighlight());
      solrQuery.setHighlightSnippets(NUM_HL_SNIPPETS);
      solrQuery.setHighlightFragsize(FRAGMENT_SIZE);
      for (NameUsageSearchRequest.HighlightField hlField : searchRequest.getHighlightFields()) {
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
          List<String> or = Lists.newArrayList();
          for (String value : params.get(param)) {
            if (Strings.isNullOrEmpty(value)) {
              throw new IllegalArgumentException("Null value not allowed for filter parameter " + param);
            }

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

            or.add(PARAMS_JOINER.join(solrField, filterVal));
          }
          if (!or.isEmpty()) {
            solrQuery.addFilterQuery(toParenthesesQuery(PARAMS_OR_JOINER.join(or)));
          }
        }
      }
    }
  }

  /**
   * Helper method that sets the parameter for a faceted query.
   *
   * @param searchRequest the searchRequest used to extract the parameters
   * @param solrQuery this object is modified by adding the facets parameters
   */
  private void requestFacets(FacetedSearchRequest<NameUsageSearchParameter> searchRequest, SolrQuery solrQuery) {

    if (!searchRequest.getFacets().isEmpty()) {
      // Only show facets that contains at least 1 record
      solrQuery.setFacet(true);
      // defaults if not overridden on per field basis
      solrQuery.setFacetMinCount(MoreObjects.firstNonNull(searchRequest.getFacetMinCount(), DEFAULT_FACET_COUNT));
      solrQuery.setFacetMissing(false);
      solrQuery.setFacetSort(DEFAULT_FACET_SORT.toString().toLowerCase());

      if(searchRequest.getFacetLimit() != null) {
        solrQuery.setFacetLimit(searchRequest.getFacetLimit());
      }

      if(searchRequest.getFacetOffset() != null) {
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
          solrQuery.addFacetField(taggedField(field, FACET_FILTER_EX));
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

}
