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
import org.gbif.api.vocabulary.Habitat;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.api.vocabulary.ThreatStatus;
import org.gbif.checklistbank.index.model.NameUsageAvro;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch.core.search.Hit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NameUsageSearchResultConverter
    implements Function<Hit<NameUsageAvro>,NameUsageSearchResult> {


  private static final String CONCAT = " # ";
  private static final Pattern LANG_SPLIT = Pattern.compile("^([a-zA-Z]*)" + CONCAT + "(.*)$");

  @Override
  public NameUsageSearchResult apply(Hit<NameUsageAvro> hit) {

    NameUsageAvro result = hit.source();

    NameUsageSearchResult u = new NameUsageSearchResult();
    u.setKey(Integer.parseInt(hit.id()));

    getHighlightOrStringValue(hit.highlight(), result.getAccepted(), "accepted").ifPresent(u::setAccepted);
    u.setAcceptedKey(result.getAcceptedKey());


    getHighlightOrStringValue(hit.highlight(), result.getCanonicalName(), "canonicalName").ifPresent(u::setCanonicalName);

    getHighlightOrStringValue(hit.highlight(), result.getAuthorship(),"authorship").ifPresent(u::setAuthorship);
    getHighlightOrStringValue(hit.highlight(), result.getAccordingTo(),"accordingTo").ifPresent(u::setAccordingTo);
    getHighlightOrStringValue(hit.highlight(), result.getPublishedIn(),"publishedIn").ifPresent(u::setPublishedIn);

    u.setNubKey(result.getNubKey());
    u.setNumDescendants(result.getNumDescendants());

    getHighlightOrStringValue(hit.highlight(), result.getParent(),"parent").ifPresent(u::setParent);
    u.setParentKey(result.getParentKey());

    getHighlightOrStringValue(hit.highlight(), result.getScientificName(), "scientificName").ifPresent(u::setScientificName);

    getHighlightOrStringValue(hit.highlight(), result.getBasionym(),"basionym").ifPresent(u::setBasionym);
    u.setBasionymKey(result.getBasionymKey());

    setClassification(hit, u, u);

    u.setNameKey(result.getNameKey());
    u.setExtinct(result.getExtinct());

    getUuidValue(result.getConstituentKey()).ifPresent(u::setConstituentKey);
    getUuidValue(result.getDatasetKey()).ifPresent(u::setDatasetKey);

    u.setTaxonID(result.getSourceId());

    getEnumListFromOrdinals(ThreatStatus.class, result.getThreatStatusKey()).ifPresent(u::setThreatStatuses);
    getEnumListFromOrdinals(NomenclaturalStatus.class, result.getNomenclaturalStatusKey()).ifPresent(u::setNomenclaturalStatus);
    getEnumListFromOrdinals(Habitat.class, result.getHabitatKey()).ifPresent(u::setHabitats);
    getEnumFromOrdinal(Origin.class, result.getOriginKey()).ifPresent(u::setOrigin);
    getEnumFromOrdinal(TaxonomicStatus.class, result.getTaxonomicStatusKey()).ifPresent(u::setTaxonomicStatus);
    getEnumFromOrdinal(NameType.class, result.getNameType()).ifPresent(u::setNameType);
    getEnumFromOrdinal(Rank.class, result.getRankKey()).ifPresent(u::setRank);


    getObjectList(result.getVernacularName(), NameUsageSearchResultConverter::deserializeVernacularName).ifPresent(u::setVernacularNames);
    getObjectList(result.getDescription(), NameUsageSearchResultConverter::deserializeDescription).ifPresent(u::setDescriptions);

    return u;
  }

   static void setClassification(Hit<NameUsageAvro> hit, LinneanClassification lc, LinneanClassificationKeys lck) {

    NameUsageAvro result = hit.source();

    getHighlightOrStringValue(hit.highlight(), result.getKingdom(), "kingdom").ifPresent(lc::setKingdom);
    lck.setKingdomKey(result.getKingdomKey());

    getHighlightOrStringValue(hit.highlight(), result.getPhylum(), "phylum").ifPresent(lc::setPhylum);
    lck.setPhylumKey(result.getPhylumKey());

    getHighlightOrStringValue(hit.highlight(), result.getClazz(), "clazz").ifPresent(lc::setClazz);
    lck.setClassKey(result.getClassKey());

    getHighlightOrStringValue(hit.highlight(), result.getOrder(), "order").ifPresent(lc::setOrder);
    lck.setOrderKey(result.getOrderKey());

    getHighlightOrStringValue(hit.highlight(), result.getFamily(),"family").ifPresent(lc::setFamily);
    lck.setFamilyKey(result.getFamilyKey());

    getHighlightOrStringValue(hit.highlight(), result.getGenus(), "genus").ifPresent(lc::setGenus);
    lck.setGenusKey(result.getGenusKey());

    getHighlightOrStringValue(hit.highlight(), result.getSubgenus(), "subgenus").ifPresent(lc::setSubgenus);
    lck.setSubgenusKey(result.getSubgenusKey());

    getHighlightOrStringValue(hit.highlight(), result.getSpecies(), "species").ifPresent(lc::setSpecies);
    lck.setSpeciesKey(result.getSpeciesKey());

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


  public static Optional<String> getHighlightOrStringValue(Map<String, List<String>> hlFields, String value, String esField) {
    Optional<String> fieldValue = Optional.ofNullable(value);
    if (Objects.nonNull(hlFields)) {
      Optional<String> hlValue =
        Optional.ofNullable(hlFields.get(esField))
          .map(hlField -> hlField.get(0));
      return hlValue.isPresent() ? hlValue : fieldValue;
    }
    return fieldValue;
  }

  public static Optional<UUID> getUuidValue(String value) {
    return Optional.ofNullable(value).map(UUID::fromString);
  }

  public static <T extends Enum<?>> Optional<List<T>> getEnumListFromOrdinals(Class<T> vocab, List<Integer> value) {
    return Optional.ofNullable(value)
      .filter(v -> !v.isEmpty())
      .map(v -> v.stream().map(val -> vocab.getEnumConstants()[val]).collect(Collectors.toList()));
  }

  public static <T extends Enum<?>> Optional<T> getEnumFromOrdinal(Class<T> vocab, Integer value) {
    return Optional.ofNullable(value)
      .map(v -> vocab.getEnumConstants()[v]);
  }

  public static <T> Optional<List<T>> getObjectList(List<String> value, Function<String,T> mapper) {
    return Optional.ofNullable(value)
      .filter(v -> !v.isEmpty())
      .map(v -> v.stream().map(mapper).collect(Collectors.toList()));
  }

}
