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
package org.gbif.nub.lookup.fuzzy;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.util.List;

import lombok.Builder;

class NameUsageBuilder {
  @Builder(builderMethodName = "builder")
  public static NameUsageMatch newNameUsageMatch(Integer usageKey,
                                                 Integer acceptedUsageKey,
                                                 String scientificName,
                                                 String canonicalName,
                                                 Rank rank,
                                                 TaxonomicStatus status,
                                                 Integer confidence,
                                                 String note,
                                                 NameUsageMatch.MatchType matchType,
                                                 List<NameUsageMatch> alternatives,
                                                 String kingdom,
                                                 String phylum,
                                                 String clazz,
                                                 String order,
                                                 String family,
                                                 String genus,
                                                 String subgenus,
                                                 String species,
                                                 Integer kingdomKey,
                                                 Integer phylumKey,
                                                 Integer classKey,
                                                 Integer orderKey,
                                                 Integer familyKey,
                                                 Integer genusKey,
                                                 Integer subgenusKey,
                                                 Integer speciesKey) {
    NameUsageMatch m = new NameUsageMatch();
    m.setUsageKey(usageKey);
    m.setAcceptedUsageKey(acceptedUsageKey);
    m.setScientificName(scientificName);
    m.setCanonicalName(canonicalName);
    m.setRank(rank);
    m.setStatus(status);
    m.setConfidence(confidence);
    m.setNote(note);
    m.setMatchType(matchType);
    m.setAlternatives(alternatives);
    m.setKingdom(kingdom);
    m.setPhylum(phylum);
    m.setClazz(clazz);
    m.setOrder(order);
    m.setFamily(family);
    m.setGenus(genus);
    m.setSubgenus(subgenus);
    m.setSpecies(species);
    m.setKingdomKey(kingdomKey);
    m.setPhylumKey(phylumKey);
    m.setClassKey(classKey);
    m.setOrderKey(orderKey);
    m.setFamilyKey(familyKey);
    m.setGenusKey(genusKey);
    m.setSubgenusKey(subgenusKey);
    m.setSpeciesKey(speciesKey);
    return m;
  }
}
