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
package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.*;
import org.gbif.api.vocabulary.Habitat;
import org.gbif.checklistbank.index.model.NameUsageAvro;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.common.parsers.HabitatParser;
import org.gbif.common.parsers.core.ParseResult;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Threadsafe class that transforms a {@link NameUsage} object into {@link NameUsageAvro}.
 */
public class NameUsageAvroConverter {

  private static final String CONCAT = " # ";

  /**
   * Logger for the {@link NameUsageAvroConverter} class
   */
  protected static final Logger LOG = LoggerFactory.getLogger(NameUsageAvroConverter.class);

  public static String serializeDescription(Description description) {
    return stripHtml(description.getDescription());
  }

  private static String stripHtml(String html) {
    if (!Strings.isNullOrEmpty(html)) {
      try {
        return Jsoup.parse(html).text();
      } catch (RuntimeException e) {
        LOG.error("Failed to read description input");
      }
    }
    return null;
  }

  public static String serializeVernacularName(VernacularName vernacularName) {
    return vernacularName.getLanguage().getIso2LetterCode() + CONCAT + vernacularName.getVernacularName();
  }

  /**
   * Takes a Generic object and transform it into a {@link NameUsageAvro}.
   *
   * @param usage container to be transformed into a {@link NameUsageAvro}.
   *              vernacular names, descriptions, species profiles and distributions are used, so populate them!
   *
   * @return a {@link NameUsageAvro} using the Object parameter.
   */
  public static NameUsageAvro toObject(NameUsage usage, List<Integer> parents, @Nullable UsageExtensions extensions, @Nullable Long occurrenceCount) {
    try {
      NameUsageAvro nameUsageAvro = new NameUsageAvro();
      nameUsageAvro.setKey(usage.getKey());
      nameUsageAvro.setNubKey(usage.getNubKey());
      if(usage.getDatasetKey() != null) {
        nameUsageAvro.setDatasetKey(usage.getDatasetKey().toString());
      }
      if(usage.getConstituentKey() != null) {
        nameUsageAvro.setConstituentKey(usage.getConstituentKey().toString());
      }
      nameUsageAvro.setParent(usage.getParent());
      nameUsageAvro.setParentKey(usage.getParentKey());
      nameUsageAvro.setAccepted(usage.getAccepted());
      nameUsageAvro.setAcceptedKey(usage.getAcceptedKey());
      nameUsageAvro.setBasionym(usage.getBasionym());
      nameUsageAvro.setBasionymKey(usage.getBasionymKey());
      nameUsageAvro.setScientificName(usage.getScientificName());
      nameUsageAvro.setCanonicalName(usage.getCanonicalName());
      nameUsageAvro.setAuthorship(usage.getAuthorship());
      nameUsageAvro.setPublishedIn(usage.getPublishedIn());
      nameUsageAvro.setAccordingTo(usage.getAccordingTo());
      nameUsageAvro.setKingdom(usage.getKingdom());
      nameUsageAvro.setKingdomKey(usage.getKingdomKey());
      nameUsageAvro.setPhylum(usage.getPhylum());
      nameUsageAvro.setPhylumKey(usage.getPhylumKey());
      nameUsageAvro.setClazz(usage.getClazz());
      nameUsageAvro.setClassKey(usage.getClassKey());
      nameUsageAvro.setOrder(usage.getOrder());
      nameUsageAvro.setOrderKey(usage.getOrderKey());
      nameUsageAvro.setFamily(usage.getFamily());
      nameUsageAvro.setFamilyKey(usage.getFamilyKey());
      nameUsageAvro.setGenus(usage.getGenus());
      nameUsageAvro.setGenusKey(usage.getGenusKey());
      nameUsageAvro.setSubgenus(usage.getSubgenus());
      nameUsageAvro.setSubgenusKey(usage.getSubgenusKey());
      nameUsageAvro.setSpecies(usage.getSpecies());
      nameUsageAvro.setSpeciesKey(usage.getSpeciesKey());
      nameUsageAvro.setNumDescendants(usage.getNumDescendants());
      nameUsageAvro.setIsSynonym(usage.isSynonym());
      nameUsageAvro.setNameKey(usage.getNameKey());

      // higher taxa
      nameUsageAvro.setHigherTaxonKey(parents);
      // enums
      if(usage.getNameType() != null) {
        nameUsageAvro.setNameType(usage.getNameType().ordinal());
      }
      nameUsageAvro.setIssues(getOrdinals(usage.getIssues()));
      nameUsageAvro.setNomenclaturalStatusKey(getOrdinals(usage.getNomenclaturalStatus()));
      if(usage.getOrigin() != null) {
        nameUsageAvro.setOriginKey(usage.getOrigin().ordinal());
      }
      if(usage.getTaxonomicStatus() != null) {
        nameUsageAvro.setTaxonomicStatusKey(usage.getTaxonomicStatus().ordinal());
      }
      if(usage.getRank() != null) {
        nameUsageAvro.setRankKey(usage.getRank().ordinal());
      }

      // extract extension infos
      if (extensions != null) {
        addVernacularNames(nameUsageAvro, extensions);
        addDescriptions(nameUsageAvro, extensions);
        addDistributionsAndThreatStatus(nameUsageAvro, extensions);
        addSpeciesProfiles(nameUsageAvro, extensions);
      }

      nameUsageAvro.setOccurrenceCount(occurrenceCount);

      return nameUsageAvro;

    } catch (Exception e) {
      LOG.error("Error converting usage {} extension {} and parent {} to avro", usage, extensions, parents, e);
      throw new RuntimeException(e);
    }
  }


  private static List<Integer> getOrdinals(Collection<? extends Enum> enums){
    List<Integer> ordinals = null;
    try {
      if (enums != null && !enums.isEmpty()) {
        ordinals = Lists.newArrayList();
        for (Enum<?> literal : enums) {
          if(literal != null) {
            ordinals.add(literal.ordinal());
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Error converting ordinals for enum", e);
    }
    return ordinals;
  }

  /**
   * Utility method that iterates over all the {@link Description} objects of a {@link NameUsage}.
   *
   * @param doc to be modified by adding the description fields
   */
  private static void addDescriptions(NameUsageAvro nameUsageAvro, UsageExtensions ext) {
    if (ext.descriptions == null) {
      return;
    }
    List<String> descriptions = Lists.newArrayList();
    for (Description description : ext.descriptions) {
      descriptions.add(serializeDescription(description));
    }

    nameUsageAvro.setDescription(descriptions);
  }

  /**
   * Utility method that iterates over all the {@link Distribution} objects of a {@link NameUsage}.
   *
   * @param doc to be modified by adding the distributions fields
   */
  private static void addDistributionsAndThreatStatus(NameUsageAvro nameUsageAvro, UsageExtensions ext) {
    if (ext.distributions == null) {
      return;
    }
    List<Integer> threatStatusKeys = Lists.newArrayList();
    for (Distribution distribution : ext.distributions) {
      if (distribution.getThreatStatus() != null) {
        threatStatusKeys.add(distribution.getThreatStatus().ordinal());
      }
    }

    nameUsageAvro.setThreatStatusKey(threatStatusKeys);
  }

  /**
   * Utility method that iterates over all the {@link org.gbif.api.model.checklistbank.SpeciesProfile} objects of a {@link NameUsage}.
   *
   * @param doc to be modified by adding the species profiles(extinct & marine) fields
   */
  private static void addSpeciesProfiles(NameUsageAvro nameUsageAvro, UsageExtensions ext) {
    if (ext.speciesProfiles == null) {
      return;
    }

    // use container logic to build a single value
    NameUsageContainer usage = new NameUsageContainer();
    usage.setSpeciesProfiles(ext.speciesProfiles);

    nameUsageAvro.setExtinct(usage.isExtinct());
    nameUsageAvro.setHabitatKey(getHabitatsKeys(usage));
  }

  private static List<Integer> getHabitatsKeys(NameUsageContainer usage){
     List<Integer> habitats = Lists.newArrayList();
    // derive habitat values from boolean flags
    addHabitat(habitats, usage.isFreshwater(), Habitat.FRESHWATER);
    addHabitat(habitats, usage.isMarine(), Habitat.MARINE);
    addHabitat(habitats, usage.isTerrestrial(), Habitat.TERRESTRIAL);
    // see if we can make use of uncontrolled habitat values with the parser, CoL uses it a lot!
    HabitatParser hp = HabitatParser.getInstance();
    for (String habitat : usage.getHabitats()) {
      ParseResult<Habitat> result = hp.parse(habitat);
      if (result.isSuccessful()) {
        addHabitat(habitats, result.getPayload());
      }
    }
    return habitats;
  }

  private static void addHabitat(List<Integer> habitats, Boolean add, Habitat habitat) {
    if (add != null && add) {
      addHabitat(habitats, habitat);
    }
  }

  private static void addHabitat(List<Integer> habitats, Habitat habitat) {
    if (habitat != null) {
     habitats.add(habitat.ordinal());
    }
  }


  /**
   * Utility method that iterates over all the {@link VernacularName} objects of a {@link NameUsage}.
   *
   * @param doc to be modified by adding the vernacular name fields
   */
  private static void addVernacularNames(NameUsageAvro nameUsageAvro, UsageExtensions ext) {
    if (ext.vernacularNames == null) {
      return;
    }
    List<String> vernacularNames = Lists.newArrayList();
    List<String> vernacularLang = Lists.newArrayList();
    List<String> vernacularNamesLang = Lists.newArrayList();

    for (VernacularName vernacularName : ext.vernacularNames) {
      vernacularNames.add(vernacularName.getVernacularName());
      if (vernacularName.getLanguage() != null) {
        vernacularLang.add(vernacularName.getLanguage().getIso2LetterCode());
        vernacularNamesLang.add(serializeVernacularName(vernacularName));
      }
    }

    nameUsageAvro.setVernacularName(vernacularNames);
    nameUsageAvro.setVernacularLang(vernacularLang);
    nameUsageAvro.setVernacularNameLang(vernacularNamesLang);
  }

}
