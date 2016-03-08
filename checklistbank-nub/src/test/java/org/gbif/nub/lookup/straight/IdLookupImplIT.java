package org.gbif.nub.lookup.straight;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nub.lookup.NubMatchingTestModule;

import java.io.IOException;

import com.google.common.base.Joiner;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class IdLookupImplIT {

  private static IdLookup matcher;
  private static final Joiner COMMA_JOINER = Joiner.on(", ").skipNulls();

  @BeforeClass
  public static void buildMatcher() throws IOException {
    matcher = NubMatchingTestModule.provideLookup();
  }

  private LookupUsage assertMatch(String name, Rank rank, Kingdom kingdom, Integer expectedKey) {
    return assertMatch(name, null, null, rank, kingdom, expectedKey);
  }

  private LookupUsage assertMatch(String name, String authorship, String year, Rank rank, Kingdom kingdom, Integer expectedKey) {
    LookupUsage m = matcher.match(name, authorship, year, rank, kingdom);
    if (expectedKey == null && m == null) {
      // all fine, as expected!
    } else if (m == null) {
      fail("\n" + name + " not matching, but expecting " + expectedKey);
    } else {
      System.out.println("\n" + name + " matches " + m.getCanonical() + " " + m.getAuthorship() + " [" + COMMA_JOINER.join(m.getKingdom(), m.getRank(), m.getKey()) + "]");
      if (expectedKey != null && m.getKey() != expectedKey) {
        printAltMatches(name);
      }
      assertEquals(expectedKey, (Integer)m.getKey());
    }
    return m;
  }

  private void printAltMatches(String name) {
    System.out.println("\nALTERNATIVES for " + name);
    for (LookupUsage m : matcher.match(name)) {
      System.out.println("\n" + m.getCanonical() + " " + m.getAuthorship() + " [" + COMMA_JOINER.join(m.getKingdom(), m.getRank(), m.getKey()) + "]");
    }
  }

  private void assertNoMatch(String name, String authorship, String year, Rank rank, Kingdom kingdom) {
    assertMatch(name, authorship, year, rank, kingdom, null);
  }


  @Test
  public void testMatching() throws IOException, InterruptedException {
    assertMatch("Abies", "", "", Rank.GENUS, Kingdom.PLANTAE, 1);

    assertNoMatch("Abies alba", null, null, Rank.SPECIES, Kingdom.ANIMALIA);
    assertNoMatch("Abies alba", null, null, Rank.GENUS, Kingdom.PLANTAE);
    assertNoMatch("Abies alba", null, null, Rank.SUBSPECIES, Kingdom.PLANTAE);
    assertMatch("Abies alba", null, null, Rank.SPECIES, Kingdom.PLANTAE, 2);
    assertMatch("Abies alba", "", "", Rank.SPECIES, Kingdom.PLANTAE, 2);
    assertMatch("Abies alba", "", "1768", Rank.SPECIES, Kingdom.PLANTAE, 2);
    assertMatch("Abies alba", "Mill.", "", Rank.SPECIES, Kingdom.PLANTAE, 2);
    assertMatch("Abies alba", "Miller", "", Rank.SPECIES, Kingdom.PLANTAE, 2);
    assertMatch("Abies alba", "Mill.", "1800", Rank.SPECIES, Kingdom.INCERTAE_SEDIS, 2);
    assertMatch("Abies alba", "nothing but a year", "1768", Rank.SPECIES, Kingdom.INCERTAE_SEDIS, 2);
    assertNoMatch("Abies alba", "DC", "", Rank.SPECIES, Kingdom.PLANTAE);
    assertNoMatch("Abies alba", "DeCandole", "1769", Rank.SPECIES, Kingdom.PLANTAE);
    assertNoMatch("Abies alba", "Linnaeus", "", Rank.SPECIES, Kingdom.PLANTAE);
    assertNoMatch("Abies alba", "L.", "1989", Rank.SPECIES, Kingdom.PLANTAE);
  }

}
