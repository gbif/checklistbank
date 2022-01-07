package org.gbif.checklistbank.nub.validation;

import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;

import javax.annotation.Nullable;

public interface AssertionEngine {

  boolean isValid();

  void assertUsage(int usageKey, Rank rank, String name, @Nullable String accepted, Kingdom kingdom);


  void assertParentsContain(String searchName, Rank searchRank, String parent);

  void assertParentsContain(int usageKey, Rank parentRank, String parent);


  void assertClassification(int usageKey, LinneanClassification classification);

  void assertClassification(int usageKey, String... classification);


  void assertSearchMatch(int expectedSearchMatches, String name);

  void assertSearchMatch(int expectedSearchMatches, String name, Rank rank);

  void assertNotExisting(String name, Rank rank);

}
