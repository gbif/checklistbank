package org.gbif.nub.lookup.fuzzy;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.service.MatchingService;

import javax.annotation.Nullable;

/**
 * dev/null implementation of an IdLookup doing nothing.
 */
public class NubMatchingPassThru implements MatchingService {
  private final NameUsageMatch empty;

  public NubMatchingPassThru() {
    empty = new NameUsageMatch();
    empty.setMatchType(NameUsageMatch.MatchType.NONE);
    empty.setConfidence(100);
  }

  @Override
  public Integer matchStrict(ParsedName pn, String scientificName, @Nullable Rank rank, @Nullable LinneanClassification classification) {
    return null;
  }

  @Override
  public NameUsageMatch match(String scientificName, @Nullable Rank rank, @Nullable LinneanClassification classification, boolean strict, boolean verbose) {
    return empty;
  }
}
