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
import org.gbif.api.util.VocabularyUtils;
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

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;

public class NameUsageEsFieldMapper implements EsFieldMapper<NameUsageSearchParameter> {

  private static final ImmutableBiMap<NameUsageSearchParameter, String> SEARCH_TO_ES_MAPPING =
      ImmutableBiMap.<NameUsageSearchParameter, String>builder()
          .put(NameUsageSearchParameter.DATASET_KEY, "datasetKey")
          .put(NameUsageSearchParameter.NAME_TYPE, "nameType")
          .put(NameUsageSearchParameter.CONSTITUENT_KEY, "constituentKey")
          .put(NameUsageSearchParameter.HIGHERTAXON_KEY, "higherTaxonKey")
          .put(NameUsageSearchParameter.HABITAT, "habitatKey")
          .put(NameUsageSearchParameter.IS_EXTINCT, "extinct")
          .put(NameUsageSearchParameter.ISSUE, "issues")
          .put(NameUsageSearchParameter.NOMENCLATURAL_STATUS, "nomenclaturalStatusKey")
          .put(NameUsageSearchParameter.ORIGIN, "originKey")
          .put(NameUsageSearchParameter.RANK, "rankKey")
          .put(NameUsageSearchParameter.STATUS, "taxonomicStatusKey")
          .put(NameUsageSearchParameter.THREAT, "threatStatusKey")
          .build();

  public static final Map<String, Integer> CARDINALITIES =
      ImmutableMap.<String, Integer>builder()
          .put("taxonomicStatusKey", TaxonomicStatus.values().length)
          .put("threatStatusKey", ThreatStatus.values().length)
          .put("issues", NameUsageIssue.values().length)
          .put("originKey", Origin.values().length)
          .put("nomenclaturalStatusKey", NomenclaturalStatus.values().length)
          .put("habitatKey", Habitat.values().length)
          .put("nameType", NameType.values().length)
          .build();

  private static final List<String> EXCLUDE_FIELDS = Collections.singletonList("all");


  private static final List<String> HIGHLIGHT_FIELDS = Arrays.asList("accepted", "parent", "basionym",
                                                                     "scientificName", "canonicalName", "authorship",
                                                                     "publishedIn", "accordingTo", "kingdom",
                                                                     "phylum", "clazz", "order",
                                                                     "family", "genus", "subgenus",
                                                                     "species", "description", "vernacularName");


  protected static final Query BOOSTING_QUERY = Query.of(qb -> qb.multiMatch(QueryBuilders.multiMatch()
                                                                              .query("0")
                                                                              .fields("taxonomicStatusKey^1.5",
                                                                                      "nameType1.5")
                                                                              .boost(10.0f)
                                                                              .build()));

  protected static final Query SPECIES_BOOSTING_QUERY = Query.of(qb -> qb.constantScore(csq -> csq.filter(Query.of(q -> q.term(QueryBuilders.term()
                                                                                                                         .field("rankKey")
                                                                                                                         .value(Rank.SPECIES.ordinal()).build())))
                                                                                                          .boost(50.0f)));

  protected static final FunctionScore BOOSTING_FUNCTION = FunctionScore.of( f -> f.fieldValueFactor(FunctionScoreBuilders.fieldValueFactor()
                                                                                    .field("rankKey")
                                                                                    .modifier(FieldValueFactorModifier.Sqrt)
                                                                                    .missing(0d).build()));

  private static final List<SortOptions> SORT = Collections.singletonList(SortOptions.of(so -> so.field(fs -> fs.field("key")
                                                  .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))));

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
  public String parseIndexedValue(String value, NameUsageSearchParameter parameter) {
    if (Enum.class.isAssignableFrom(parameter.type()) && StringUtils.isNumeric(value)) {
      return ((Class<Enum<?>>)parameter.type()).getEnumConstants()[Integer.parseInt(value)].name();
    }
    return EsFieldMapper.super.parseIndexedValue(value, parameter);
  }

  @Override
  public String parseParamValue(String value, NameUsageSearchParameter parameter) {
    if (Enum.class.isAssignableFrom(parameter.type())) {
      return VocabularyUtils.lookup(value, (Class<Enum<?>>) parameter.type())
        .map(e -> Integer.toString(e.ordinal()))
        .orElse(null);
    }
    return EsFieldMapper.super.parseParamValue(value, parameter);
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
      "originKey",
      "nomenclaturalStatusKey",
      "taxonomicStatusKey",
      "threatStatusKey",
      "rankKey",
      "habitatKey",
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
      "higherTaxonKey",
      "issues");
  }

  @Override
  public Query fullTextQuery(String q) {
    return Query.of(mq -> mq.functionScore(
              QueryBuilders.functionScore()
                .functions(BOOSTING_FUNCTION)
                .query(qb ->  qb.bool(BoolQuery.of( bool ->
                    bool.must(mf -> mf.bool(sf -> sf.should(sb -> sb.multiMatch(mm ->
                          mm.query(q)
                            .fields("description^0.1",
                                    "vernacularName^3",
                                    "canonicalName^8",
                                    "scientificName^10",
                                    "genus",
                                    "species",
                                    "subgenus",
                                    "family")
                            .tieBreaker(0.2d)
                            .minimumShouldMatch("1")
                            .slop(3)
                        ))
                      .should(phraseQuery(q, 25f))
                      .should(SPECIES_BOOSTING_QUERY)
                      .should(BOOSTING_QUERY))
                ))))
                .build()));
  }

  public Query phraseQuery(String q, Float boost) {
    return Query.of(qb -> qb.multiMatch(QueryBuilders.multiMatch()
                            .query(q)
                            .fields("description^2.0",
                                    "vernacularName^20.0",
                                    "scientificName^100.0",
                                    "canonicalName^50.0")
                            .tieBreaker(0.2d)
                            .minimumShouldMatch("1")
                            .slop(100)
                            .type(TextQueryType.Phrase)
                            .boost(boost)
                            .build()));
  }

}
