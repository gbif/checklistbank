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


import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;

public class NameUsageSuggestEsFieldMapper extends NameUsageEsFieldMapper {

  @Override
  public Query fullTextQuery(String q) {
    return Query.of(qb -> qb.functionScore(fsb -> fsb.functions(BOOSTING_FUNCTION)
                                                      .query(Query.of(bool -> bool.bool(QueryBuilders.bool()
                                                                                        .must(suggestQuery(q))
                                                                                        .should(suggestPhraseQuery(q))
                                                                                        .should(SPECIES_BOOSTING_QUERY)
                                                                                        .should(BOOSTING_QUERY).build())))
                                                        ));
  }

  private Query suggestQuery(String q) {
    return  Query.of(qb -> qb.multiMatch(QueryBuilders.multiMatch().query(q)
                                            .fields("canonicalNameNgram^15",
                                                    "canonicalNameTokenized^6",
                                                    "canonicalNameNgramTokenized^2",
                                                    "scientificName^5")
                                            .minimumShouldMatch("1")
                                            .slop(2)
                                           .build()));
  }

  private Query suggestPhraseQuery(String q) {
    return Query.of(qb -> qb.multiMatch(QueryBuilders.multiMatch().query(q)
                                          .fields("canonicalNameNgram^15.0")
                                          .slop(2)
                                          .boost(10f)
                                          .build()));
  }

  public static void main(String[] args) {
    System.out.println(new NameUsageSuggestEsFieldMapper().fullTextQuery("puma concolor (Linnaeus, 1771)"));
  }

}
