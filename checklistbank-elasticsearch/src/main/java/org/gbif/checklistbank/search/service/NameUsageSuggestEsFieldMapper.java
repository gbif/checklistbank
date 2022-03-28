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


import org.gbif.api.vocabulary.Rank;

import java.util.Arrays;
import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;

public class NameUsageSuggestEsFieldMapper extends NameUsageEsFieldMapper {

  @Override
  public Query fullTextQuery(String q) {
    Rank boostingRank = isPhrase(q)? Rank.SPECIES: Rank.GENUS;
    return Query.of(mq -> mq.bool(bool -> { bool.must(suggestQuery(q))
                                                    .should(suggestPhraseQuery(q))
                                                    .should(rankBoostingFunction(boostingRank))
                                                    .should(rankBoostingQuery(boostingRank))
                                                    .should(BOOSTING_QUERY);
                                              return bool;
    }));
  }

  @Override
  public List<String> getMappedFields() {
    return Arrays.asList(
      "key",
      "nameKey",
      "nubKey",
      "parentKey",
      "parent",
      "scientificName",
      "canonicalName",
      "taxonomicStatusKey",
      "rankKey",
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
      "species");
  }

  private Query suggestQuery(String q) {
    return  Query.of(qb -> qb.multiMatch(QueryBuilders.multiMatch().query(q)
                                            .fields("canonicalNameTokenized^10",
                                                    "canonicalNameNgram^5",
                                                    "canonicalNameNgramTokenized^2",
                                                    "scientificName")
                                            .minimumShouldMatch("1")
                                            .type(isPhrase(q)? TextQueryType.PhrasePrefix : TextQueryType.MostFields)
                                            .slop(2)
                                           .build()));
  }

  private Query suggestPhraseQuery(String q) {
    return Query.of(qb -> isPhrase(q)? qb.matchPhrase(QueryBuilders.matchPhrase().query(q)
                                          .field("canonicalName")
                                          .boost(10.0f)
                                          .slop(2)
                                          .build()) : qb.match(QueryBuilders.match().query(q)
                                          .field("canonicalName")
                                          .boost(10.0f)
                                          .build()));
  }
}
