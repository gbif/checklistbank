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
import org.gbif.api.model.checklistbank.search.NameUsageSuggestResult;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NameUsageSearchResultConverter
    implements SearchResultConverter<NameUsageSearchResult, NameUsageSuggestResult> {

  private static final Pattern NESTED_PATTERN = Pattern.compile("^\\w+(\\.\\w+)+$");
  private static final Predicate<String> IS_NESTED = s -> NESTED_PATTERN.matcher(s).find();

  private static final String CONCAT = " # ";
  private static final Pattern LANG_SPLIT = Pattern.compile("^([a-zA-Z]*)" + CONCAT + "(.*)$");

  @Override
  public NameUsageSearchResult toSearchResult(SearchHit hit) {

    Map<String, Object> fields = hit.getSourceAsMap();
    NameUsageSearchResult u = new NameUsageSearchResult();
    u.setKey(Integer.parseInt(hit.getId()));

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "accepted").ifPresent(u::setAccepted);
    getIntValue(fields, "accepted_key").ifPresent(u::setAcceptedKey);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "canonical_name").ifPresent(u::setCanonicalName);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "authorship").ifPresent(u::setAuthorship);
    getHighlightOrStringValue(fields, hit.getHighlightFields(), "according_to").ifPresent(u::setAccordingTo);
    getHighlightOrStringValue(fields, hit.getHighlightFields(), "published_in").ifPresent(u::setPublishedIn);

    getIntValue(fields, "nub_key").ifPresent(u::setNubKey);
    getIntValue(fields, "num_descendants").ifPresent(u::setNumDescendants);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "parent").ifPresent(u::setParent);
    getIntValue(fields, "parent_key").ifPresent(u::setParentKey);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "scientific_name").ifPresent(u::setScientificName);

    getHighlightOrStringValue(fields, hit.getHighlightFields(), "basionym").ifPresent(u::setBasionym);
    getIntValue(fields, "basionym_key").ifPresent(u::setBasionymKey);

    setClassification(fields, hit, u, u);

    getIntValue(fields, "name_key").ifPresent(u::setNameKey);
    getBooleanValue(fields, "extinct").ifPresent(u::setExtinct);

    getUuidValue(fields, "constituent_key").ifPresent(u::setConstituentKey);
    getUuidValue(fields, "dataset_key").ifPresent(u::setDatasetKey);

    getStringValue(fields, "source_id").ifPresent(u::setTaxonID);

    getEnumListFromOrdinals(ThreatStatus.class, fields, "threat_status_key").ifPresent(u::setThreatStatuses);
    getEnumListFromOrdinals(NomenclaturalStatus.class, fields, "nomenclatural_status_key").ifPresent(u::setNomenclaturalStatus);
    getEnumListFromOrdinals(Habitat.class, fields, "habitat_key").ifPresent(u::setHabitats);
    getEnumFromOrdinal(Origin.class, fields, "origin").ifPresent(u::setOrigin);
    getEnumFromOrdinal(TaxonomicStatus.class, fields, "taxonomic_status_key").ifPresent(u::setTaxonomicStatus);
    getEnumFromOrdinal(NameType.class, fields, "name_type").ifPresent(u::setNameType);
    getEnumFromOrdinal(Rank.class, fields, "rank_key").ifPresent(u::setRank);


    getObjectList(fields, "vernacular_name", NameUsageSearchResultConverter::deserializeVernacularName).ifPresent(u::setVernacularNames);
    getObjectList(fields, "descriptions", NameUsageSearchResultConverter::deserializeDescription).ifPresent(u::setDescriptions);

    return u;
  }

  private void setClassification(Map<String,Object> fields, SearchHit hit, LinneanClassification lc, LinneanClassificationKeys lck) {
    for (Rank r : Rank.DWC_RANKS) {
      getHighlightOrStringValue(fields, hit.getHighlightFields(), r.name().toLowerCase())
        .ifPresent(val -> ClassificationUtils.setHigherRank(lc, r, val));

      getIntValue(fields, r.name().toLowerCase() + "_key")
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

  @Override
  public NameUsageSuggestResult toSearchSuggestResult(SearchHit hit) {
    NameUsageSuggestResult u = new NameUsageSuggestResult();

    Map<String, Object> fields = hit.getSourceAsMap();
    u.setKey(Integer.parseInt(hit.getId()));

    getIntValue(fields, "name_key").ifPresent(u::setNameKey);
    getIntValue(fields, "nub_key").ifPresent(u::setNubKey);
    getIntValue(fields, "parent_key").ifPresent(u::setParentKey);

    getStringValue(fields, "parent").ifPresent(u::setParent);
    getStringValue(fields, "scientific_name").ifPresent(u::setScientificName);
    getStringValue(fields, "canonical_name").ifPresent(u::setCanonicalName);

    getEnumFromOrdinal(TaxonomicStatus.class, fields, "taxonomic_status_key").ifPresent(u::setStatus);
    getEnumFromOrdinal(Rank.class, fields, "rank_key").ifPresent(u::setRank);

    setClassification(fields, hit, u, u);

    return u;
  }


  private static <T extends Enum<?>> Optional<List<T>> getEnumListFromOrdinals(Class<T> vocab, Map<String, Object> fields, String field) {
    return Optional.ofNullable(fields.get(field))
      .map(v -> (List<Integer>) v)
      .filter(v -> !v.isEmpty())
      .map(v -> v.stream().map(val -> vocab.getEnumConstants()[val]).collect(Collectors.toList()));
  }

  private static <T> Optional<List<T>> getObjectList(Map<String, Object> fields, String field, Function<String,T> mapper) {
    return Optional.ofNullable(fields.get(field))
      .map(v -> (List<String>) v)
      .filter(v -> !v.isEmpty())
      .map(v -> v.stream().map(mapper::apply).collect(Collectors.toList()));
  }

  private static <T extends Enum<?>> Optional<T> getEnumFromOrdinal(Class<T> vocab, Map<String, Object> fields, String field) {
    return Optional.ofNullable(fields.get(field))
      .map(v -> (Integer) v)
      .map(v -> vocab.getEnumConstants()[v]);
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

  private static Optional<UUID> getUuidValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, UUID::fromString);
  }

  private static Optional<String> getStringValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, Function.identity());
  }

  private static Optional<String> getHighlightOrStringValue(
      Map<String, Object> fields, Map<String, HighlightField> hlFields, String esField) {
    Optional<String> fieldValue = getValue(fields, esField, Function.identity());
    if (Objects.nonNull(hlFields)) {
      Optional<String> hlValue =
          Optional.ofNullable(hlFields.get(esField))
              .map(hlField -> hlField.getFragments()[0].string());
      return hlValue.isPresent() ? hlValue : fieldValue;
    }
    return fieldValue;
  }

  private static Optional<Integer> getIntValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, Integer::valueOf);
  }

  private static Optional<Boolean> getBooleanValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, Boolean::valueOf);
  }

  private static <T> Optional<T> getValue(
      Map<String, Object> fields, String esField, Function<String, T> mapper) {
    String fieldName = esField;
    if (IS_NESTED.test(esField)) {
      // take all paths till the field name
      String[] paths = esField.split("\\.");
      for (int i = 0; i < paths.length - 1 && fields.containsKey(paths[i]); i++) {
        // update the fields with the current path
        fields = (Map<String, Object>) fields.get(paths[i]);
      }
      // the last path is the field name
      fieldName = paths[paths.length - 1];
    }

    return extractValue(fields, fieldName, mapper);
  }

  private static <T> Optional<T> extractValue(
      Map<String, Object> fields, String fieldName, Function<String, T> mapper) {
    return Optional.ofNullable(fields.get(fieldName))
        .map(String::valueOf)
        .filter(v -> !v.isEmpty())
        .map(
            v -> {
              try {
                return mapper.apply(v);
              } catch (Exception ex) {
                log.error("Error extracting field {} with value {}", fieldName, v);
                return null;
              }
            });
  }
}
