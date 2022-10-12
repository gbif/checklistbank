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
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.*;
import org.gbif.checklistbank.index.model.NameUsageAvro;
import org.gbif.common.search.EsSearchRequestBuilder;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;

import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NameUsageSearchResultConverter
    implements Function<Hit<NameUsageAvro>, NameUsageSearchResult> {

  private static final String CONCAT = " # ";
  private static final Pattern LANG_SPLIT = Pattern.compile("^([a-zA-Z]*)" + CONCAT + "(.*)$");

  @Override
  public NameUsageSearchResult apply(Hit<NameUsageAvro> hit) {

    NameUsageAvro result = hit.source();

    NameUsageSearchResult u = new NameUsageSearchResult();
    u.setKey(Integer.parseInt(hit.id()));

    getHighlightOrStringValue(hit.highlight(), result.getAccepted(), "accepted")
        .ifPresent(u::setAccepted);
    u.setAcceptedKey(result.getAcceptedKey());

    getHighlightOrStringValue(hit.highlight(), result.getCanonicalName(), "canonicalName")
        .ifPresent(u::setCanonicalName);

    getHighlightOrStringValue(hit.highlight(), result.getAuthorship(), "authorship")
        .ifPresent(u::setAuthorship);
    getHighlightOrStringValue(hit.highlight(), result.getAccordingTo(), "accordingTo")
        .ifPresent(u::setAccordingTo);
    getHighlightOrStringValue(hit.highlight(), result.getPublishedIn(), "publishedIn")
        .ifPresent(u::setPublishedIn);

    u.setNubKey(result.getNubKey());
    u.setNumDescendants(result.getNumDescendants());

    getHighlightOrStringValue(hit.highlight(), result.getParent(), "parent")
        .ifPresent(u::setParent);
    u.setParentKey(result.getParentKey());

    getHighlightOrStringValue(hit.highlight(), result.getScientificName(), "scientificName")
        .ifPresent(u::setScientificName);

    getHighlightOrStringValue(hit.highlight(), result.getBasionym(), "basionym")
        .ifPresent(u::setBasionym);
    u.setBasionymKey(result.getBasionymKey());

    setClassification(hit, u, u);

    u.setNameKey(result.getNameKey());
    u.setExtinct(result.getExtinct());

    getUuidValue(result.getConstituentKey()).ifPresent(u::setConstituentKey);
    getUuidValue(result.getDatasetKey()).ifPresent(u::setDatasetKey);

    u.setTaxonID(result.getSourceId());

    getEnumList(ThreatStatus.class, result.getThreatStatus()).ifPresent(u::setThreatStatuses);
    getEnumList(NomenclaturalStatus.class, result.getNomenclaturalStatus())
        .ifPresent(u::setNomenclaturalStatus);
    getEnumList(Habitat.class, result.getHabitat()).ifPresent(u::setHabitats);

    VocabularyUtils.lookup(result.getOrigin(), Origin.class).ifPresent(u::setOrigin);
    VocabularyUtils.lookup(result.getTaxonomicStatus(), TaxonomicStatus.class)
        .ifPresent(u::setTaxonomicStatus);
    VocabularyUtils.lookup(result.getNameType(), NameType.class).ifPresent(u::setNameType);
    VocabularyUtils.lookup(result.getRank(), Rank.class).ifPresent(u::setRank);

    getVernacularName(result.getVernacularNameLang(), hit.highlight())
        .ifPresent(u::setVernacularNames);
    getDescription(result.getDescription(), hit.highlight()).ifPresent(u::setDescriptions);

    return u;
  }

  static void setClassification(
      Hit<NameUsageAvro> hit, LinneanClassification lc, LinneanClassificationKeys lck) {

    NameUsageAvro result = hit.source();

    getHighlightOrStringValue(hit.highlight(), result.getKingdom(), "kingdom")
        .ifPresent(lc::setKingdom);
    lck.setKingdomKey(result.getKingdomKey());

    getHighlightOrStringValue(hit.highlight(), result.getPhylum(), "phylum")
        .ifPresent(lc::setPhylum);
    lck.setPhylumKey(result.getPhylumKey());

    getHighlightOrStringValue(hit.highlight(), result.getClazz(), "clazz").ifPresent(lc::setClazz);
    lck.setClassKey(result.getClassKey());

    getHighlightOrStringValue(hit.highlight(), result.getOrder(), "order").ifPresent(lc::setOrder);
    lck.setOrderKey(result.getOrderKey());

    getHighlightOrStringValue(hit.highlight(), result.getFamily(), "family")
        .ifPresent(lc::setFamily);
    lck.setFamilyKey(result.getFamilyKey());

    getHighlightOrStringValue(hit.highlight(), result.getGenus(), "genus").ifPresent(lc::setGenus);
    lck.setGenusKey(result.getGenusKey());

    getHighlightOrStringValue(hit.highlight(), result.getSubgenus(), "subgenus")
        .ifPresent(lc::setSubgenus);
    lck.setSubgenusKey(result.getSubgenusKey());

    getHighlightOrStringValue(hit.highlight(), result.getSpecies(), "species")
        .ifPresent(lc::setSpecies);
    lck.setSpeciesKey(result.getSpeciesKey());
  }

  private static Description deserializeDescription(String description) {
    Description d = new Description();
    d.setDescription(description);
    return d;
  }

  private Optional<List<Description>> getDescription(
      List<String> descriptions, Map<String, List<String>> hlFields) {
    if (descriptions == null || descriptions.isEmpty()) {
      return Optional.empty();
    }

    Map<String, String> hValuesWithSource = getHlValuesWithSource(hlFields, "description");
    return Optional.of(
        descriptions.stream()
            .map(v -> hValuesWithSource.getOrDefault(v, v))
            .map(NameUsageSearchResultConverter::deserializeDescription)
            .collect(Collectors.toList()));
  }

  private Optional<List<VernacularName>> getVernacularName(
      List<String> nameLangs, Map<String, List<String>> hlFields) {
    if (nameLangs == null || nameLangs.isEmpty()) {
      return Optional.empty();
    }

    Map<String, String> hNamesWithSource = getHlValuesWithSource(hlFields, "vernacularName");
    return Optional.of(
        nameLangs.stream()
            .map(
                nl -> {
                  Matcher m = LANG_SPLIT.matcher(nl);
                  VernacularName vn = new VernacularName();
                  if (m.find()) {
                    vn.setLanguage(Language.fromIsoCode(m.group(1)));
                    String name = m.group(2);
                    vn.setVernacularName(hNamesWithSource.getOrDefault(name, name));
                  } else {
                    vn.setVernacularName(hNamesWithSource.getOrDefault(nl, nl));
                  }
                  return vn;
                })
            .collect(Collectors.toList()));
  }

  private Map<String, String> getHlValuesWithSource(
      Map<String, List<String>> hlFields, String esField) {
    Map<String, String> hValuesWithSource = new HashMap<>();
    if (Objects.nonNull(hlFields)) {
      Optional<List<String>> hlValue = Optional.ofNullable(hlFields.get(esField));

      if (hlValue.isPresent()) {
        // merge values
        hValuesWithSource =
            hlValue.get().stream()
                .collect(
                    Collectors.toMap(
                        v ->
                            StringEscapeUtils.unescapeHtml(
                                v.replaceAll(EsSearchRequestBuilder.PRE_HL_TAG, "")
                                    .replaceAll(EsSearchRequestBuilder.POST_HL_TAG, "")),
                        v -> v,
                        (v1, v2) -> v1));
      }
    }
    return hValuesWithSource;
  }

  public static Optional<String> getHighlightOrStringValue(
      Map<String, List<String>> hlFields, String value, String esField) {
    Optional<String> fieldValue = Optional.ofNullable(value);
    if (Objects.nonNull(hlFields)) {
      Optional<String> hlValue =
          Optional.ofNullable(hlFields.get(esField)).map(hlField -> hlField.get(0));
      return hlValue.isPresent() ? hlValue : fieldValue;
    }
    return fieldValue;
  }

  public static Optional<UUID> getUuidValue(String value) {
    return Optional.ofNullable(value).map(UUID::fromString);
  }

  public static <T extends Enum<?>> Optional<List<T>> getEnumList(
      Class<T> vocab, List<String> value) {
    return Optional.ofNullable(value)
        .filter(v -> !v.isEmpty())
        .map(
            v ->
                v.stream()
                    .map(val -> VocabularyUtils.lookupEnum(val, vocab))
                    .collect(Collectors.toList()));
  }
}
