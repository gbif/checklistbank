package org.gbif.nub.build;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.checklistbank.model.Usage;

/**
 * Add some extra infos only needed during nub processing
 */
public class NubUsage  {
  public NameUsageMatch.MatchType matchType;
  public int confidence;
  public boolean directParentMatch;
  public Usage usage;

  public NubUsage () {
    usage = new Usage();
  }

  public NubUsage (Usage u, boolean directParentMatch, NameUsageMatch.MatchType matchType, int confidence) {
    this.usage = u;
    this.directParentMatch = directParentMatch;
    this.matchType = matchType;
    this.confidence = confidence;
  }
}
