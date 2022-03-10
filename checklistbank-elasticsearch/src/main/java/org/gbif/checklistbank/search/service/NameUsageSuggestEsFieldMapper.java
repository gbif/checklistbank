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


import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;

public class NameUsageSuggestEsFieldMapper extends NameUsageEsFieldMapper {

  @Override
  public QueryBuilder fullTextQuery(String q) {
    return new FunctionScoreQueryBuilder(QueryBuilders.boolQuery()
                                           .must(suggestPhraseQuery(q))
                                           .should(suggestQuery(q))
                                           .should(SPECIES_BOOSTING_QUERY)
                                           .should(BOOSTING_QUERY), BOOSTING_FUNCTION);
  }

  private QueryBuilder suggestQuery(String q) {
    return  QueryBuilders.multiMatchQuery(q)
              .field("canonicalNameTokenized", 6.0f)
              .field("canonicalNameNgramTokenized", 2.0f)
              .minimumShouldMatch("1")
              .slop(2);
  }

  private QueryBuilder suggestPhraseQuery(String q) {
    return QueryBuilders.multiMatchQuery(q)
            .field("canonicalNameNgram", 15.0f)
            .field("scientificName", 5.0f)
            .slop(2)
            .boost(10);
  }

  public static void main(String[] args) {
    System.out.println(new NameUsageSuggestEsFieldMapper().fullTextQuery("puma concolor (Linnaeus, 1771)"));
  }

}
