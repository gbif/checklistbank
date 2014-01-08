package org.gbif.nub.lookup;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nameparser.NameParser;

import java.io.IOException;

import org.apache.commons.lang.math.IntRange;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NubMatchingServiceImplIT {

  private static NubMatchingServiceImpl matcher;

  @BeforeClass
  public static void buildMatcher() throws IOException {
    matcher = new NubMatchingServiceImpl(NubMatchingTestModule.provideIndex(), NubMatchingTestModule.provideSynonyms(), new NameParser());
  }

  private void assertMatch(String name, LinneanClassification query, Integer expectedKey, IntRange confidence) {
    NameUsageMatch best = matcher.match(name, null, query, false, true);
    System.out.println(
      name + " matches " + best.getScientificName() + " [" + best.getUsageKey() + "] with confidence " + best
        .getConfidence());
    System.out.println(best.getNote());

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
    assertMatchConsistency(best);
  }

  private void assertNoMatch(String name, LinneanClassification query, IntRange confidence) {
    NameUsageMatch best = matcher.match(name, null, query, false, true);
    System.out.println(best.getNote());
    assertEquals(NameUsageMatch.MatchType.NONE, best.getMatchType());
    if (best.getAlternatives() != null && !best.getAlternatives().isEmpty()) {
      NameUsageMatch alt = best.getAlternatives().get(0);
      for (NameUsageMatch m : best.getAlternatives()) {
        System.out.println("  Alt: " + m.getScientificName() + " [" + m.getUsageKey() + "] score=" + m.getConfidence() + ". " + m.getNote());
      }
      if (confidence != null) {
        assertTrue("alt confidence " + alt.getConfidence() + " not within " + confidence, confidence.containsInteger(alt.getConfidence()));
      }
    }
  }


  private void assertMatchConsistency(NameUsageMatch match){
    assertNotNull(match.getConfidence());
    assertNotNull(match.getMatchType());
    if (NameUsageMatch.MatchType.NONE == match.getMatchType()){
      assertNull(match.getUsageKey());
      assertNull(match.getScientificName());

      assertNull(match.getSpeciesKey());
      assertNull(match.getGenusKey());
      assertNull(match.getFamilyKey());
      assertNull(match.getOrderKey());
      assertNull(match.getClassKey());
      assertNull(match.getPhylumKey());
      assertNull(match.getKingdomKey());

      assertNull(match.getSpecies());
      assertNull(match.getGenus());
      assertNull(match.getFamily());
      assertNull(match.getOrder());
      assertNull(match.getClazz());
      assertNull(match.getPhylum());
      assertNull(match.getKingdom());

    } else {
      assertNotNull(match.getUsageKey());
      assertNotNull(match.getScientificName());

      if(match.getRank() != null){
        Rank rank = match.getRank();
        if (rank.isSuprageneric()){
          assertNull(match.getSpecies());
          assertNull(match.getSpeciesKey());
          assertNull(match.getGenus());
          assertNull(match.getGenusKey());

        } else if (rank == Rank.GENUS){
          assertNotNull(match.getGenus());
          assertNull(match.getSpecies());
          assertNull(match.getSpeciesKey());

        } else if (rank == Rank.SPECIES){
          assertNotNull(match.getGenus());
          assertNotNull(match.getSpecies());
          assertNotNull(match.getSpeciesKey());
          if (!match.isSynonym()){
            assertEquals(match.getUsageKey(), match.getSpeciesKey());
            assertTrue(match.getScientificName().startsWith(match.getSpecies()));
          }

        } else if (rank.isInfraspecific()){
          assertNotNull(match.getGenus());
          assertNotNull(match.getSpecies());
          assertNotNull(match.getSpeciesKey());
          if (!match.isSynonym()){
            assertFalse(match.getUsageKey().equals(match.getSpeciesKey()));
            assertTrue(match.getScientificName().startsWith(match.getSpecies()));
          }

        }
      }
    }
  }

  private void assertNubIdNotNullAndNotEqualToAnyHigherRank(NameUsageMatch x) {
    assertNotNull(x.getUsageKey());
    assertFalse(x.getUsageKey() == x.getKingdomKey());
    assertFalse(x.getUsageKey() == x.getPhylumKey());
    assertFalse(x.getUsageKey() == x.getClassKey());
    assertFalse(x.getUsageKey() == x.getOrderKey());
    assertFalse(x.getUsageKey() == x.getFamilyKey());
  }

  @Test
  public void testMatching() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    assertMatch("Anephlus", cl, 1100135, new IntRange(92,93));
    assertMatch("Aneplus", cl, 1100050, new IntRange(92,94));

    cl.setKingdom("Animalia");
    cl.setClazz("Insecta");
    assertMatch("Aneplus", cl, 1100050, new IntRange(97,100));

    // genus Aneplus is order=Coleoptera, but Anelus is a Spirobolida in class Diplopoda
    cl.setClazz("Diplopoda");
    cl.setOrder("Spirobolida");
    assertMatch("Aneplus", cl, 1027792, new IntRange(92, 95));

    cl.setFamily("Atopetholidae");
    assertMatch("Aneplus", cl, 1027792, new IntRange(99, 100));
    assertMatch("Annepluss", cl, 1027792, new IntRange(95,99));

    assertNoMatch("Annepluss", new NameUsageMatch(), new IntRange(30,80));
  }

  @Test
  public void testHomonyms() throws IOException {
    // Oenanthe
    LinneanClassification cl = new NameUsageMatch();
    assertNoMatch("Oenanthe", cl, null);

    cl.setKingdom("Animalia");
    assertMatch("Oenanthe", cl, 2492483, new IntRange(96, 99));

    cl.setKingdom("Plantae");
    assertMatch("Oenanthe", cl, 3034893, new IntRange(96, 99));
    assertMatch("Oenante", cl, 3034893, new IntRange(95,99));


    // Acanthophora
    cl = new NameUsageMatch();
    assertNoMatch("Acanthophora", cl, null);

    cl.setKingdom("Animalia");
    assertMatch("Acanthophora", cl, 3251480, new IntRange(93,97));

    cl.setKingdom("Plantae"); // there are multiple plant genera, this should match to plantae now
    assertMatch("Acanthophora", cl, 6, new IntRange(98,100));

    cl.setFamily("Araliaceae");
    assertMatch("Acanthophora", cl, 3036337, new IntRange(98, 100));
    assertMatch("Acantophora", cl, 3036337, new IntRange(96,100)); // fuzzy match
    // try matching with authors
    assertMatch("Acantophora Merrill", cl, 3036337, new IntRange(97, 100)); // fuzzy match

    cl.setFamily("Rhodomelaceae");
    assertMatch("Acanthophora", cl, 2659277, new IntRange(97, 100));


    // species match
    cl = new NameUsageMatch();
    assertMatch("Puma concolor", cl, 2435099, new IntRange(98,100));

    cl.setGenus("Puma");
    assertMatch("P. concolor", cl, 2435099, new IntRange(98, 100));

    cl.setKingdom("Animalia");
    assertMatch("P. concolor", cl, 2435099, new IntRange(99, 100));

    cl.setKingdom("Plantae");
    assertMatch("Puma concolor", cl, 2435099, new IntRange(95, 98));
    // Picea concolor is a plant, but this fuzzy match is too far off
    assertMatch("Pima concolor", cl, 6, null);
    // this one should match though
    assertMatch("Pica concolor", cl, 5284657, new IntRange(92, 97));

    cl.setFamily("Pinaceae");
    assertMatch("Pima concolor", cl, 5284657, new IntRange(93, 96));


    // Amphibia is a homonym genus, but also and most prominently a class!
    cl = new NameUsageMatch();
    // non existing "species" name. Amphibia could either be the genus or the class, who knows...
    assertNoMatch("Amphibia eyecount", cl, null);

    // first try a match against the algae genus
    cl.setKingdom("Plantae");
    cl.setClazz("Rhodophyceae");
    assertMatch("Amphibia eyecount", cl, 2659299, new IntRange(90, 99));

    // now try with the animal class
    cl.setKingdom("Animalia");
    cl.setClazz("Amphibia");
    assertMatch("Amphibia eyecount", cl, 131, new IntRange(98, 100));
  }

  @Test
  @Ignore("not implemented yet")
  public void testHybridsAndViruses() throws IOException {
    //TODO: implement
  }

}
