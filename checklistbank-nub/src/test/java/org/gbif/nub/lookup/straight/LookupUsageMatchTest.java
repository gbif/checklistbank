package org.gbif.nub.lookup.straight;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.utils.SerdeTestUtils;

import org.junit.Test;

/**
 *
 */
public class LookupUsageMatchTest {

  @Test
  public void testSerde() throws Exception {
    LookupUsage u = new LookupUsage();
    u.setKey(321);
    u.setAuthorship("Mill.");
    u.setCanonical("Abies alba");
    u.setRank(Rank.UNRANKED);
    u.setKingdom(Kingdom.INCERTAE_SEDIS);
    u.setYear("1999");

    LookupUsageMatch m = new LookupUsageMatch();
    m.setMatch(u);

    SerdeTestUtils.testSerDe(m, LookupUsageMatch.class);
  }
}