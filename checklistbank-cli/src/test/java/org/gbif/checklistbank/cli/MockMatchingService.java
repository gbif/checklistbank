package org.gbif.checklistbank.cli;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.vocabulary.Rank;

import javax.annotation.Nullable;

/**
 * Mock matching service configured to always return none matches.
 */
public class MockMatchingService implements NameUsageMatchingService {

  @Override
  public NameUsageMatch match(String scientificName, @Nullable Rank rank,
    @Nullable LinneanClassification classification, boolean strict, boolean verbose) {

    NameUsageMatch match = new NameUsageMatch();
    match.setConfidence(0);
    match.setMatchType(NameUsageMatch.MatchType.NONE);
    return match;
  }
}
