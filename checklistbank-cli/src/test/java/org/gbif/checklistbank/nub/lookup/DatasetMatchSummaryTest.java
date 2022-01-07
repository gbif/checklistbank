package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.vocabulary.Rank;

import org.junit.Ignore;
import org.junit.Test;

import java.util.UUID;

@Ignore("REMOVE! ignored only to make the jenkins build work")
public class DatasetMatchSummaryTest {
  @Test
  public void toStringTest() throws Exception {
    DatasetMatchSummary summary = new DatasetMatchSummary(UUID.randomUUID());
    summary.addNoMatch(Rank.KINGDOM);
    summary.addNoMatch(Rank.CLASS);
    summary.addNoMatch(Rank.CLASS);
    summary.addNoMatch(Rank.FORM);
    summary.addNoMatch(Rank.BIOVAR);
    summary.addNoMatch(Rank.SUBGENUS);
    summary.addNoMatch(Rank.UNRANKED);
    summary.addNoMatch(null);

    summary.addMatch(Rank.ORDER);
    summary.addMatch(Rank.FAMILY);
    summary.addMatch(Rank.GENUS);
    summary.addMatch(Rank.SPECIES);
    summary.addMatch(Rank.VARIETY);
    summary.addMatch(Rank.SPECIES);
    summary.addMatch(Rank.SUBSPECIES);
    summary.addMatch(null);

    System.out.println(summary);
  }

}
