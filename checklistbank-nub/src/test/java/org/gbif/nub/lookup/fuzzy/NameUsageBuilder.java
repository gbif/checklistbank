package org.gbif.nub.lookup.fuzzy;

import lombok.Builder;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.util.List;

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
