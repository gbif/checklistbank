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
import org.gbif.common.search.SearchResultConverter;

import java.util.Map;

import org.elasticsearch.search.SearchHit;

import lombok.extern.slf4j.Slf4j;

import static org.gbif.checklistbank.search.service.NameUsageSearchResultConverter.setClassification;
import static org.gbif.common.search.es.EsConversionUtils.getEnumFromOrdinal;
import static org.gbif.common.search.es.EsConversionUtils.getIntValue;
import static org.gbif.common.search.es.EsConversionUtils.getStringValue;

@Slf4j
public class NameUsageSearchSuggestResultConverter
    implements SearchResultConverter<NameUsageSuggestResult> {


  @Override
  public NameUsageSuggestResult toSearchResult(SearchHit hit) {
    NameUsageSuggestResult u = new NameUsageSuggestResult();

    Map<String, Object> fields = hit.getSourceAsMap();
    u.setKey(Integer.parseInt(hit.getId()));

    getIntValue(fields, "nameKey").ifPresent(u::setNameKey);
    getIntValue(fields, "nubKey").ifPresent(u::setNubKey);
    getIntValue(fields, "parentKey").ifPresent(u::setParentKey);

    getStringValue(fields, "parent").ifPresent(u::setParent);
    getStringValue(fields, "scientificName").ifPresent(u::setScientificName);
    getStringValue(fields, "canonicalName").ifPresent(u::setCanonicalName);

    getEnumFromOrdinal(TaxonomicStatus.class, fields, "taxonomicStatusKey").ifPresent(u::setStatus);
    getEnumFromOrdinal(Rank.class, fields, "rankKey").ifPresent(u::setRank);

    setClassification(fields, hit, u, u);

    return u;
  }
}