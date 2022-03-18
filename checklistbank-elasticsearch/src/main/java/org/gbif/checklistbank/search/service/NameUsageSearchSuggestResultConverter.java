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

import org.gbif.api.model.checklistbank.search.NameUsageSuggestResult;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.index.model.NameUsageAvro;

import java.util.function.Function;

import co.elastic.clients.elasticsearch.core.search.Hit;

import lombok.extern.slf4j.Slf4j;

import static org.gbif.checklistbank.search.service.NameUsageSearchResultConverter.setClassification;
import static org.gbif.checklistbank.search.service.NameUsageSearchResultConverter.getEnumFromOrdinal;

@Slf4j
public class NameUsageSearchSuggestResultConverter
    implements Function<Hit<NameUsageAvro>,NameUsageSuggestResult> {


  @Override
  public NameUsageSuggestResult apply(Hit<NameUsageAvro> hit) {
    NameUsageSuggestResult u = new NameUsageSuggestResult();

    NameUsageAvro result = hit.source();
    u.setKey(Integer.parseInt(hit.id()));

    u.setNameKey(result.getNameType());
    u.setNubKey(result.getNubKey());
    u.setParentKey(result.getParentKey());

    u.setParent(result.getParent());
    u.setScientificName(result.getScientificName());
    u.setCanonicalName(result.getCanonicalName());

    getEnumFromOrdinal(TaxonomicStatus.class, result.getTaxonomicStatusKey()).ifPresent(u::setStatus);
    getEnumFromOrdinal(Rank.class, result.getRankKey()).ifPresent(u::setRank);

    setClassification(hit, u, u);

    return u;
  }
}
