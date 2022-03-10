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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

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

  private static final String[] EXCLUDE_FIELDS = new String[] {"all"};


  private static final String[] HIGHLIGHT_FIELDS = new String[] {"accepted", "parent", "basionym",
                                                                "scientificName", "canonicalName", "authorship",
                                                                "publishedIn", "accordingTo", "kingdom",
                                                                "phylum", "clazz", "order",
                                                                "family", "genus", "subgenus",
                                                                "species", "description", "vernacularName"};

  protected static final QueryBuilder BOOSTING_QUERY = QueryBuilders.multiMatchQuery("0")
                                                                    .field("taxonomicStatusKey", 1.5f)
                                                                    .field("nameType",1.5f)
                                                                    .boost(10);

  protected static final QueryBuilder SPECIES_BOOSTING_QUERY = QueryBuilders.constantScoreQuery(QueryBuilders.termQuery("rankKey",
                                                                                                                        Rank.SPECIES.ordinal()))
                                                                            .boost(50);

  protected static final FieldValueFactorFunctionBuilder BOOSTING_FUNCTION =
    ScoreFunctionBuilders.fieldValueFactorFunction("rankKey")
                         .modifier(FieldValueFactorFunction.Modifier.LN2P)
                         .missing(0d);

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
      "issues"
    };
  }

  @Override
  public QueryBuilder fullTextQuery(String q) {
    return
      new FunctionScoreQueryBuilder(
        QueryBuilders.disMaxQuery().add(
            QueryBuilders.multiMatchQuery(q)
                .field("description", 0.1f)
                .field("vernacularName", 3.0f)
                .field("canonicalName", 10.0f)
                .field("scientificName", 12.0f)
                .field("genus")
                .field("species")
                .field("subgenus")
                .field("family")
                .tieBreaker(0.2f)
                .minimumShouldMatch("1")
                .slop(3)
                .boost(50))
          .add(phraseQuery(q).boost(25))
          .add(SPECIES_BOOSTING_QUERY)
          .add(BOOSTING_QUERY),
        BOOSTING_FUNCTION);
  }

  public QueryBuilder phraseQuery(String q) {
    return QueryBuilders.multiMatchQuery(q)
            .field("description", 2.0f)
            .field("vernacularName", 20.0f)
            .field("scientificName", 100.0f)
            .field("canonicalName", 50.0f)
            .tieBreaker(0.2f)
            .minimumShouldMatch("1")
            .slop(100)
            .type(MultiMatchQueryBuilder.Type.PHRASE);
  }

}
