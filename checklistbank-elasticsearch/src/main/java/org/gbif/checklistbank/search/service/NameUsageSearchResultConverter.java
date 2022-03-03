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

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.checklistbank.search.NameUsageSearchResult;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.model.common.LinneanClassificationKeys;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Habitat;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.api.vocabulary.ThreatStatus;
import org.gbif.common.search.SearchResultConverter;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.search.SearchHit;

import lombok.extern.slf4j.Slf4j;

import static org.gbif.common.search.es.EsConversionUtils.getBooleanValue;
import static org.gbif.common.search.es.EsConversionUtils.getEnumFromOrdinal;
import static org.gbif.common.search.es.EsConversionUtils.getEnumListFromOrdinals;
import static org.gbif.common.search.es.EsConversionUtils.getHighlightOrStringValue;
import static org.gbif.common.search.es.EsConversionUtils.getIntValue;
import static org.gbif.common.search.es.EsConversionUtils.getObjectList;
import static org.gbif.common.search.es.EsConversionUtils.getStringValue;
import static org.gbif.common.search.es.EsConversionUtils.getUuidValue;

@Slf4j
public class NameUsageSearchResultConverter
    implements SearchResultConverter<NameUsageSearchResult> {


  private static final String CONCAT = " # ";
  private static final Pattern LANG_SPLIT = Pattern.compile("^([a-zA-Z]*)" + CONCAT + "(.*)$");

  @Override
  public NameUsageSearchResult toSearchResult(SearchHit hit) {

    Map<String, Object> fields = hit.getSourceAsMap();
    NameUsageSearchResult u = new NameUsageSearchResult();
    u.setKey(Integer.parseInt(hit.getId()));

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "accepted").ifPresent(u::setAccepted);
    getIntValue(fields, "acceptedKey").ifPresent(u::setAcceptedKey);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "canonicalName").ifPresent(u::setCanonicalName);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "authorship").ifPresent(u::setAuthorship);
    getHighlightOrStringValue(fields, hit.getHighlightFields(), "accordingTo").ifPresent(u::setAccordingTo);
    getHighlightOrStringValue(fields, hit.getHighlightFields(), "publishedIn").ifPresent(u::setPublishedIn);

    getIntValue(fields, "nubKey").ifPresent(u::setNubKey);
    getIntValue(fields, "numDescendants").ifPresent(u::setNumDescendants);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "parent").ifPresent(u::setParent);
    getIntValue(fields, "parentKey").ifPresent(u::setParentKey);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "scientificName").ifPresent(u::setScientificName);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "basionym").ifPresent(u::setBasionym);
    getIntValue(fields, "basionymKey").ifPresent(u::setBasionymKey);

    setClassification(fields, hit, u, u);

    getIntValue(fields, "nameKey").ifPresent(u::setNameKey);
    getBooleanValue(fields, "extinct").ifPresent(u::setExtinct);

    getUuidValue(fields, "constituentKey").ifPresent(u::setConstituentKey);
    getUuidValue(fields, "datasetKey").ifPresent(u::setDatasetKey);

    getStringValue(fields, "sourceId").ifPresent(u::setTaxonID);

    getEnumListFromOrdinals(ThreatStatus.class, fields, "threatStatusKey").ifPresent(u::setThreatStatuses);
    getEnumListFromOrdinals(NomenclaturalStatus.class, fields, "nomenclaturalStatusKey").ifPresent(u::setNomenclaturalStatus);
    getEnumListFromOrdinals(Habitat.class, fields, "habitatKey").ifPresent(u::setHabitats);
    getEnumFromOrdinal(Origin.class, fields, "origin").ifPresent(u::setOrigin);
    getEnumFromOrdinal(TaxonomicStatus.class, fields, "taxonomicStatusKey").ifPresent(u::setTaxonomicStatus);
    getEnumFromOrdinal(NameType.class, fields, "nameType").ifPresent(u::setNameType);
    getEnumFromOrdinal(Rank.class, fields, "rankKey").ifPresent(u::setRank);


    getObjectList(fields, "vernacularName", NameUsageSearchResultConverter::deserializeVernacularName).ifPresent(u::setVernacularNames);
    getObjectList(fields, "descriptions", NameUsageSearchResultConverter::deserializeDescription).ifPresent(u::setDescriptions);

    return u;
  }

   static void setClassification(Map<String,Object> fields, SearchHit hit, LinneanClassification lc, LinneanClassificationKeys lck) {
    for (Rank r : Rank.DWC_RANKS) {
      getHighlightOrStringValue(fields, hit.getHighlightFields(), r == Rank.CLASS? "clazz" : r.name().toLowerCase())
        .ifPresent(val -> ClassificationUtils.setHigherRank(lc, r, val));

      getIntValue(fields, r.name().toLowerCase() + "Key")
        .ifPresent(val -> ClassificationUtils.setHigherRankKey(lck, r, val));
    }
    /*getHighlightOrStringValue(fields, hit.getHighlightFields(), "kingdom").ifPresent(lc::setKingdom);
    getIntValue(fields, "kingdom_key").ifPresent(lck::setKingdomKey);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "phylum").ifPresent(lc::setPhylum);
    getIntValue(fields, "phylum_key").ifPresent(ulck::setPhylumKey);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "class").ifPresent(lc::setClazz);
    getIntValue(fields, "class_key").ifPresent(lck::setClassKey);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "order").ifPresent(lc::setOrder);
    getIntValue(fields, "order_key").ifPresent(lck::setOrderKey);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "family").ifPresent(lc::setFamily);
    getIntValue(fields, "family_key").ifPresent(lck::setFamilyKey);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "genus").ifPresent(lc::setGenus);
    getIntValue(fields, "genus_key").ifPresent(lck::setGenusKey);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "subgenus").ifPresent(lc::setSubgenus);
    getIntValue(fields, "subgenus_key").ifPresent(lck::setSubgenusKey);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "species").ifPresent(lc::setSpecies);
    getIntValue(fields, "species_key").ifPresent(lck::setSpeciesKey);*/
  }


  private static Description deserializeDescription(String description) {
    Description d = new Description();
    d.setDescription(description);
    return d;
  }

  private static VernacularName deserializeVernacularName(String vernacularName) {
    Matcher m = LANG_SPLIT.matcher(vernacularName);
    VernacularName vn = new VernacularName();
    if (m.find()) {
      vn.setLanguage(Language.fromIsoCode(m.group(1)));
      vn.setVernacularName(m.group(2));
    } else {
      vn.setVernacularName(vernacularName);
    }
    return vn;
  }


}
