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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;

import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;

public class NameUsageEsFieldMapper implements EsFieldMapper<NameUsageSearchParameter> {

  private static final ImmutableBiMap<NameUsageSearchParameter, String> SEARCH_TO_ES_MAPPING =
      ImmutableBiMap.<NameUsageSearchParameter, String>builder()
          .put(NameUsageSearchParameter.DATASET_KEY, "datasetKey")
          .put(NameUsageSearchParameter.NAME_TYPE, "nameType")
          .put(NameUsageSearchParameter.CONSTITUENT_KEY, "constituentKey")
          .put(NameUsageSearchParameter.HIGHERTAXON_KEY, "higherTaxonKey")
          .put(NameUsageSearchParameter.HABITAT, "habitat")
          .put(NameUsageSearchParameter.IS_EXTINCT, "extinct")
          .put(NameUsageSearchParameter.ISSUE, "issues")
          .put(NameUsageSearchParameter.NOMENCLATURAL_STATUS, "nomenclaturalStatus")
          .put(NameUsageSearchParameter.ORIGIN, "origin")
          .put(NameUsageSearchParameter.RANK, "rank")
          .put(NameUsageSearchParameter.STATUS, "taxonomicStatus")
          .put(NameUsageSearchParameter.THREAT, "threatStatus")
          .build();

  public static final Map<String, Integer> CARDINALITIES =
      ImmutableMap.<String, Integer>builder()
          .put("taxonomicStatus", TaxonomicStatus.values().length)
          .put("threatStatus", ThreatStatus.values().length)
          .put("issues", NameUsageIssue.values().length)
          .put("origin", Origin.values().length)
          .put("nomenclaturalStatus", NomenclaturalStatus.values().length)
          .put("habitat", Habitat.values().length)
          .put("nameType", NameType.values().length)
          .put("extinct", 2)
          .put("isSynonym", 2)
          .put("rank", Rank.values().length)
          .build();

  private static final List<String> EXCLUDE_FIELDS = Collections.singletonList("all");


  private static final List<String> HIGHLIGHT_FIELDS = Arrays.asList("accepted", "parent", "basionym",
                                                                     "scientificName", "canonicalName", "authorship",
                                                                     "publishedIn", "accordingTo", "kingdom",
                                                                     "phylum", "clazz", "order",
                                                                     "family", "genus", "subgenus",
                                                                     "species", "description", "vernacularName");


  protected static final Query BOOSTING_QUERY = Query.of(qb -> qb.boosting(QueryBuilders.boosting()
                                                                             .positive(p -> p.bool( pb ->
                                                                              pb.should(Query.of(t -> t.match(QueryBuilders.match().query(TaxonomicStatus.ACCEPTED.name()).field("taxonomicStatus").build())),
                                                                                        Query.of(t -> t.match(QueryBuilders.match().query(NameType.SCIENTIFIC.name()).field("nameType").build())),
                                                                                        Query.of(m -> m.match(QueryBuilders.match().query(false).field("isSynonym").build()))
                                                                                ).boost(50.0f)))
                                                                             .negative(n -> n.bool(bool -> bool.mustNot(Query.of(t -> t.match(QueryBuilders.match().query(TaxonomicStatus.ACCEPTED.name()).field("taxonomicStatus").build())))
                                                                                                               .mustNot(Query.of(t -> t.match(QueryBuilders.match().query(NameType.SCIENTIFIC.name()).field("nameType").build())))
                                                                                                               .should(Query.of(m -> m.match(QueryBuilders.match().query(true).field("isSynonym").build())))))
                                                                             .negativeBoost(75.0)
                                                                             .build()));

  private static final List<SortOptions> SORT = Collections.singletonList(SortOptions.of(so -> so.field(fs -> fs.field("key")
                                                  .order(SortOrder.Asc))));

  protected static boolean isPhrase(String q) {
    return q.trim().indexOf(' ') > 0;
  }

  protected Query rankBoostingFunction(Rank rank) {
    return Query.of(qb -> qb.scriptScore(ss -> ss.query(q -> q.exists(e -> e.field("rank")))
      .script(Script.of(s -> s.inline(is -> is.source("decayNumericGauss(" + rank.ordinal() + ",1,1,0.2,doc['rankKey'].value)")))).boost(250.0f)));
  }

  protected Query rankBoostingQuery(Rank rank) {
    return Query.of(qb -> qb.boosting(QueryBuilders.boosting()
                                        .positive(p -> p.bool(pb -> pb.must(Query.of(t -> t.match(QueryBuilders.match().query(rank.name()).field("rank").build())))
                                          .boost(100.0f)))
                                        .negative(n -> n.bool(bool -> bool.mustNot(Query.of(t -> t.match(QueryBuilders.match().query(rank.name()).field("rank").build())))))
                                        .negativeBoost(150.0)
                                        .build()));
  }

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
  public List<String> excludeFields() {
    return EXCLUDE_FIELDS;
  }

  @Override
  public List<SortOptions> sorts() {
    return SORT;
  }

  @Override
  public List<String> includeSuggestFields(NameUsageSearchParameter searchParameter) {
    return Collections.singletonList(SEARCH_TO_ES_MAPPING.get(searchParameter));
  }

  @Override
  public List<String> highlightingFields() {
    return HIGHLIGHT_FIELDS;
  }

  @Override
  public List<String> getMappedFields() {
    return Arrays.asList(
      "key",
      "nameKey",
      "nubKey",
      "datasetKey",
      "constituentKey",
      "parentKey",
      "parent",
      "acceptedKey",
      "accepted",
      "basionymKey",
      "basionym",
      "scientificName",
      "canonicalName",
      "nameType",
      "authorship",
      "origin",
      "nomenclaturalStatus",
      "taxonomicStatus",
      "threatStatus",
      "rank",
      "habitat",
      "publishedIn",
      "accordingTo",
      "kingdomKey",
      "kingdom",
      "phylumKey",
      "phylum",
      "classKey",
      "clazz",
      "orderKey",
      "order",
      "familyKey",
      "family",
      "genusKey",
      "genus",
      "subgenusKey",
      "subgenus",
      "speciesKey",
      "species",
      "numDescendants",
      "sourceId",
      "extinct",
      "description",
      "vernacularName",
      "vernacularNameLang",
      "higherTaxonKey",
      "issues");
  }

  @Override
  public Query fullTextQuery(String q) {
    boolean isPhrase = isPhrase(q);
    Rank boostingRank = isPhrase? Rank.SPECIES : Rank.GENUS;
    return  Query.of(qb -> qb.bool(BoolQuery.of(bool -> bool.must(mf -> mf.multiMatch(mm -> mm.query(q)
                                                                                              .fields("description^0.1",
                                                                                                      "vernacularName",
                                                                                                      "canonicalName",
                                                                                                      "scientificName",
                                                                                                      "species",
                                                                                                      "subgenus",
                                                                                                      "family")
                                                                                              .minimumShouldMatch("1")
                                                                                              .type(isPhrase? TextQueryType.PhrasePrefix : TextQueryType.BestFields)
                                                                                          ))
                                                                                        .should(phraseQuery(q))
                                                                                        .should(rankBoostingFunction(boostingRank))
                                                                                        .should(rankBoostingQuery(boostingRank))
                                                                                        .should(BOOSTING_QUERY))));
  }

  public Query phraseQuery(String q) {
    return Query.of(qb -> qb.multiMatch(QueryBuilders.multiMatch()
                            .query(q)
                            .fields("description^2.0",
                                    "vernacularName^20.0",
                                    "scientificName^100.0",
                                    "canonicalName^50.0")
                            .minimumShouldMatch("1")
                            .type(TextQueryType.PhrasePrefix)
                            .build()));
  }

}
