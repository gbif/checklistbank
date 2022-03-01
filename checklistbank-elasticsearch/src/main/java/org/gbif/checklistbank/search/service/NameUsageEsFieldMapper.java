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
import org.gbif.api.vocabulary.Habitat;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.api.vocabulary.ThreatStatus;
import org.gbif.common.search.EsFieldMapper;

import java.util.Map;

import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.BoostingQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;

public class NameUsageEsFieldMapper implements EsFieldMapper<NameUsageSearchParameter> {

  private static final ImmutableBiMap<NameUsageSearchParameter, String> SEARCH_TO_ES_MAPPING =
      ImmutableBiMap.<NameUsageSearchParameter, String>builder()
          .put(NameUsageSearchParameter.DATASET_KEY, "dataset_key")
          .put(NameUsageSearchParameter.NAME_TYPE, "name_type")
          .put(NameUsageSearchParameter.CONSTITUENT_KEY, "constituent_key")
          .put(NameUsageSearchParameter.HIGHERTAXON_KEY, "higher_taxon_key")
          .put(NameUsageSearchParameter.HABITAT, "habitat_key")
          .put(NameUsageSearchParameter.IS_EXTINCT, "extinct")
          .put(NameUsageSearchParameter.ISSUE, "issues")
          .put(NameUsageSearchParameter.NOMENCLATURAL_STATUS, "nomenclatural_status_key")
          .put(NameUsageSearchParameter.ORIGIN, "origin_key")
          .put(NameUsageSearchParameter.RANK, "rank_key")
          .put(NameUsageSearchParameter.STATUS, "taxonomic_status_key")
          .put(NameUsageSearchParameter.THREAT, "threat_status_key")
          .build();

  public static final Map<String, Integer> CARDINALITIES =
      ImmutableMap.<String, Integer>builder()
          .put("taxonomic_status_key", TaxonomicStatus.values().length)
          .put("threat_status_key", ThreatStatus.values().length)
          .put("issues", NameUsageIssue.values().length)
          .put("origin_key", Origin.values().length)
          .put("nomenclatural_status_key", NomenclaturalStatus.values().length)
          .put("habitat_key", Habitat.values().length)
          .put("name_type.type", NameType.values().length)
          .build();

  private static final String[] EXCLUDE_FIELDS = new String[] {"all"};


  private static final String[] HIGHLIGHT_FIELDS = new String[] {"accepted", "parent", "basionym",
                                                                "scientific_name", "canonical_name", "authorship",
                                                                "published_in", "according_to", "kingdom",
                                                                "phylum", "class", "order",
                                                                "family", "genus", "subgenus",
                                                                "species", "description", "vernacular_name"};

  private static final BoostingQueryBuilder BOOSTING_QUERY = QueryBuilders.boostingQuery(QueryBuilders.multiMatchQuery("0")
                                                                                          .field("taxonomic_status_key", 1.5f)
                                                                                          .field("name_type",1.5f),
                                                                                         QueryBuilders.boolQuery()
                                                                                           .mustNot(QueryBuilders.multiMatchQuery("0")
                                                                                                      .field("taxonomic_status_key")
                                                                                                      .field("name_type")))
                                                              .negativeBoost(0.5f);

  private static final ScriptScoreFunctionBuilder BOOSTING_FUNCTION =
    ScoreFunctionBuilders.scriptFunction("2 * (" + Rank.values().length + " - doc['rank_key'])");

  private static final FieldSortBuilder[] SORT =
      new FieldSortBuilder[] {
        SortBuilders.fieldSort("key").order(SortOrder.DESC)
      };

  @Override
  public NameUsageSearchParameter get(String esField) {
    return SEARCH_TO_ES_MAPPING.inverse().get(esField);
  }

  @Override
  public boolean isDateField(String esFieldName) {
    return false;
  }

  @Override
  public Integer getCardinality(String esFieldName) {
    return CARDINALITIES.get(esFieldName);
  }

  @Override
  public String get(NameUsageSearchParameter nameUsageSearchParameter) {
    return SEARCH_TO_ES_MAPPING.get(nameUsageSearchParameter);
  }

  @Override
  public String[] excludeFields() {
    return EXCLUDE_FIELDS;
  }

  @Override
  public SortBuilder<? extends SortBuilder>[] sorts() {
    return SORT;
  }

  @Override
  public String[] includeSuggestFields(NameUsageSearchParameter searchParameter) {
    return new String[] {SEARCH_TO_ES_MAPPING.get(searchParameter)};
  }

  @Override
  public String[] highlightingFields() {
    return HIGHLIGHT_FIELDS;
  }

  @Override
  public String[] getMappedFields() {
    return new String[] {
      "key",
      "name_key",
      "nub_key",
      "dataset_key",
      "constituent_key",
      "parent_key",
      "parent",
      "accepted_key",
      "accepted",
      "basionym_key",
      "basionym",
      "scientific_name",
      "canonical_name",
      "name_type",
      "authorship",
      "origin_key",
      "nomenclatural_status_key",
      "taxonomic_status_key",
      "threat_status_key",
      "rank_key",
      "habitat_key",
      "published_in",
      "according_to",
      "kingdom_key",
      "kingdom",
      "phylum_key",
      "phylum",
      "class_key",
      "class",
      "order_key",
      "order",
      "family_key",
      "family",
      "genus_key",
      "genus",
      "subgenus_key",
      "subgenus",
      "species_key",
      "species",
      "num_descendants",
      "source_id",
      "extinct",
      "description",
      "vernacular_name",
      "higher_taxon_key",
      "issues"
    };
  }

  @Override
  public QueryBuilder fullTextQuery(String q) {
    return new FunctionScoreQueryBuilder(
        QueryBuilders.boolQuery().should(
            QueryBuilders.multiMatchQuery(q)
                .field("description", 0.1f)
                .field("vernacular_name", 3.0f)
                .field("canonical_name", 10.0f)
                .field("scientific_name", 2.0f)
                .field("species")
                .field("subgenus")
                .field("family")
                .tieBreaker(0.2f)
                .minimumShouldMatch("25%")
                .slop(3))
          .should(phraseQuery(q))
          .should(getBoostingQuery())
    ).boostMode(CombineFunction.MULTIPLY);
  }



  public QueryBuilder phraseQuery(String q) {
    return new FunctionScoreQueryBuilder(
      QueryBuilders.multiMatchQuery(q)
        .field("description", 2.0f)
        .field("vernacular_name", 20.0f)
        .field("scientific_name", 100.0f)
        .field("canonical_name", 50.0f)
        .tieBreaker(0.2f)
        .minimumShouldMatch("25%")
        .slop(100))
      .boostMode(CombineFunction.MULTIPLY);
  }

  /**
   *  private static final String SUGGEST_QUERY_FIELDS = "canonical_name_tokenized^10 canonical_name_ngram^5 canonical_name_ngram_tokenized^2 scientific_name";
   *   private static final String SUGGEST_PHRASE_FIELDS = "canonical_name^10";
   */

  public QueryBuilder suggestQuery(String q) {
    return new FunctionScoreQueryBuilder(
      QueryBuilders.multiMatchQuery(q)
        .field("canonical_name_tokenized", 10.0f)
        .field("canonical_name_ngram", 5.0f)
        .field("canonical_name_ngram_tokenized", 2.0f)
        .field("scientific_name")
        .tieBreaker(0.2f)
        .minimumShouldMatch("25%")
        .slop(2))
      .boostMode(CombineFunction.MULTIPLY);
  }

  public QueryBuilder suggestPhraseQuery(String q) {
    return QueryBuilders
            .matchPhraseQuery("canonical_name", q)
            .slop(2);
  }

  public static BoostingQueryBuilder getBoostingQuery() {
    return BOOSTING_QUERY;
  }

}
