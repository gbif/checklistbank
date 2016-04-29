package org.gbif.nub.lookup.fuzzy;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nameparser.NameParser;
import org.gbif.nub.lookup.NubMatchingTestModule;

import java.io.IOException;
import javax.annotation.Nullable;

import com.google.common.base.Joiner;
import org.apache.commons.lang.math.IntRange;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class NubMatchingServiceImplStrictIT {

  private static NubMatchingServiceImpl matcher;
  private static final Joiner JOINER = Joiner.on("; ").useForNull("???");

  @BeforeClass
  public static void buildMatcher() throws IOException {
    matcher = new NubMatchingServiceImpl(NubMatchingTestModule.provideIndex(), NubMatchingTestModule.provideSynonyms(), new NameParser());
  }

  private NameUsageMatch query(String name, Rank rank, Kingdom kingdom) {
    LinneanClassification cl = new NameUsageMatch();
    cl.setKingdom(kingdom.name());

    return matcher.match(name, rank, cl, true, true);
  }

  private void assertMatch(String name, Rank rank, Kingdom kingdom, Integer expectedKey) {
    assertMatch(name, rank, kingdom, expectedKey, null);
  }

  private void assertMatch(String name, Rank rank, Kingdom kingdom, Integer expectedKey, @Nullable IntRange confidence) {
    NameUsageMatch best = query(name, rank, kingdom);

    System.out.println("\n" + name + " matches " + best.getScientificName() + " [" + best.getUsageKey() + "] with confidence " + best.getConfidence());
    System.out.println("  " + JOINER.join(best.getKingdom(), best.getPhylum(), best.getClazz(), best.getOrder(), best.getFamily()));
    System.out.println("  " + best.getNote());

    assertTrue("Wrong match type", best.getMatchType() != NameUsageMatch.MatchType.NONE);
    if (best.getAlternatives() != null) {
      for (NameUsageMatch m : best.getAlternatives()) {
        System.out.println("  Alt: " + m.getScientificName() + " [" + m.getUsageKey() + "] score=" + m.getConfidence() + ". " + m.getNote());
      }
    }
    assertEquals( expectedKey, best.getUsageKey());
    if (confidence != null) {
      assertTrue("confidence " + best.getConfidence() + " not within " + confidence, confidence.containsInteger(best.getConfidence()));
    }
    NubMatchingServiceImplIT.assertMatchConsistency(best);
  }

  private void assertNoMatch(String name, Rank rank, Kingdom kingdom) {
    NameUsageMatch best = query(name, rank, kingdom);

    System.out.println(best.getNote());
    assertEquals(NameUsageMatch.MatchType.NONE, best.getMatchType());
    if (best.getAlternatives() != null && !best.getAlternatives().isEmpty()) {
      for (NameUsageMatch m : best.getAlternatives()) {
        System.out.println("  Alt: " + m.getScientificName() + " [" + m.getUsageKey() + "] score=" + m.getConfidence() + ". " + m.getNote());
      }
    }
  }

  @Test
  public void testMatching() throws IOException, InterruptedException {
    assertMatch("Abies", Rank.GENUS, Kingdom.PLANTAE, 2684876);

    assertNoMatch("Abies alba", Rank.SPECIES, Kingdom.ANIMALIA);
    assertNoMatch("Abies alba", Rank.GENUS, Kingdom.PLANTAE);
    assertNoMatch("Abies alba", Rank.SUBSPECIES, Kingdom.PLANTAE);

    assertMatch("Abies alba", Rank.SPECIES, Kingdom.PLANTAE, 2685484);
    assertMatch("Abies alba", Rank.SPECIES, Kingdom.PLANTAE, 2685484);
    assertMatch("Abies alba 1768", Rank.SPECIES, Kingdom.PLANTAE, 2685484);
    assertMatch("Abies alba Mill.", Rank.SPECIES, Kingdom.PLANTAE, 2685484);
    assertMatch("Abies alba Miller", Rank.SPECIES, Kingdom.PLANTAE, 2685484);
    assertMatch("Abies alba Mill., 1800", Rank.SPECIES, Kingdom.INCERTAE_SEDIS, 2685484);
    assertMatch("Abies alba nothing but a year, 1768", Rank.SPECIES, Kingdom.INCERTAE_SEDIS, 2685484);
    assertNoMatch("Abies alba DC", Rank.SPECIES, Kingdom.PLANTAE);
    assertNoMatch("Abies alba DeCandole, 1769", Rank.SPECIES, Kingdom.PLANTAE);
    assertNoMatch("Abies alba Linnaeus", Rank.SPECIES, Kingdom.PLANTAE);
    assertNoMatch("Abies alba L., 1989", Rank.SPECIES, Kingdom.PLANTAE);
  }


}
