package org.gbif.nub.lookup;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nameparser.NameParser;

import java.io.IOException;

import com.google.common.base.Joiner;
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
  private static final Joiner CLASS_JOINER = Joiner.on("; ").useForNull("???");

  @BeforeClass
  public static void buildMatcher() throws IOException {
    matcher = new NubMatchingServiceImpl(NubMatchingTestModule.provideIndex(), NubMatchingTestModule.provideSynonyms(), new NameParser());
  }

  private void assertMatch(String name, LinneanClassification query, Integer expectedKey, IntRange confidence) {
    NameUsageMatch best = matcher.match(name, null, query, false, true);
    System.out.println("\n" + name + " matches " + best.getScientificName() + " [" + best.getUsageKey() + "] with confidence " + best.getConfidence());
    System.out.println("  " + CLASS_JOINER.join(best.getKingdom(), best.getPhylum(), best.getClazz(), best.getOrder(), best.getFamily()));
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
    assertFalse(x.getUsageKey().equals(x.getKingdomKey()));
    assertFalse(x.getUsageKey().equals(x.getPhylumKey()));
    assertFalse(x.getUsageKey().equals(x.getClassKey()));
    assertFalse(x.getUsageKey().equals(x.getOrderKey()));
    assertFalse(x.getUsageKey().equals(x.getFamilyKey()));
  }

  @Test
  public void testMatching() throws IOException, InterruptedException {
    LinneanClassification cl = new NameUsageMatch();
    assertMatch("Anephlus", cl, 1100135, new IntRange(90,95));
    assertMatch("Aneplus", cl, 1100050, new IntRange(90,94));

    cl.setKingdom("Animalia");
    cl.setClazz("Insecta");
    assertMatch("Aneplus", cl, 1100050, new IntRange(97,100));

    // genus Aneplus is order=Coleoptera, but Anelus is a Spirobolida in class Diplopoda
    cl.setClazz("Diplopoda");
    cl.setOrder("Spirobolida");
    assertMatch("Aneplus", cl, 1027792, new IntRange(90, 99));

    cl.setFamily("Atopetholidae");
    assertMatch("Aneplus", cl, 1027792, new IntRange(98, 100));
    // too far off
    assertMatch("Annepluss", cl, 1, new IntRange(90,100));

    assertNoMatch("Annepluss", new NameUsageMatch(), new IntRange(30,80));
  }

  /**
   * Sabia parviflora is a plant which is in our backbone badly classified as an animal.
   * Assure those names get matched to the next highest taxon that indeed is a plant when the plant kingdom is requested.
   */
  @Test
  public void testBadPlantKingdom() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    // without kingdom snap to the bad animal record
    assertMatch("Sabia parviflora", cl, 7268473, new IntRange(99,100));

    // hit the plant family
    assertMatch("Sabiaceae", cl, 2409, new IntRange(90,100));

    // make sure its the family
    cl.setKingdom("Plantae");
    cl.setFamily("Sabiaceae");
    assertMatch("Sabia parviflora", cl, 2409, new IntRange(80,100));



    // without kingdom snap to the bad animal record
    cl = new NameUsageMatch();
    assertMatch("Tibetia tongolensis", cl, 7301567, new IntRange(99,100));

    // hit the plant family
    assertMatch("Fabaceae", cl, 5386, new IntRange(90,100));

    // make sure its the family
    cl.setKingdom("Plantae");
    cl.setFamily("Fabaceae");
    assertMatch("Tibetia tongolensis", cl, 5386, new IntRange(80,100));
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
    assertMatch("Oenante", cl, 3034893, new IntRange(85,95));


    // Acanthophora
    cl = new NameUsageMatch();
    assertNoMatch("Acanthophora", cl, null);

    // there are 3 genera in animalia, 2 synonyms and 1 accepted.
    // We prefer to match to the single accepted in this case
    cl.setKingdom("Animalia");
    assertMatch("Acanthophora", cl, 3251480, new IntRange(92, 99));

    // now try with molluscs to just get the doubtful one
    cl.setPhylum("Porifera");
    assertMatch("Acanthophora", cl, 3251480, new IntRange(97,99));

    cl.setKingdom("Plantae"); // there are multiple plant genera, this should match to plantae now
    assertMatch("Acanthophora", cl, 6, new IntRange(95,100));

    cl.setFamily("Araliaceae");
    assertMatch("Acanthophora", cl, 3036337, new IntRange(98, 100));
    assertMatch("Acantophora", cl, 3036337, new IntRange(96,100)); // fuzzy match
    // try matching with authors
    assertMatch("Acantophora Merrill", cl, 3036337, new IntRange(95, 100)); // fuzzy match

    cl.setFamily("Rhodomelaceae");
    assertMatch("Acanthophora", cl, 2659277, new IntRange(97, 100));


    // species match
    cl = new NameUsageMatch();
    assertMatch("Puma concolor", cl, 2435099, new IntRange(98,100));

    cl.setGenus("Puma");
    assertMatch("P. concolor", cl, 2435099, new IntRange(98, 100));

    cl.setKingdom("Animalia");
    assertMatch("P. concolor", cl, 2435099, new IntRange(99, 100));

    cl.setKingdom("Pllllaaaantae");
    assertMatch("Puma concolor", cl, 2435099, new IntRange(95, 100));

    // we now match to the kingdom even though it was given wrong
    // sideeffect of taking kingdoms extremely serious due to bad backbone
    // see NubMatchingServiceImpl.classificationSimilarity()
    cl.setKingdom("Plantae");
    assertMatch("Puma concolor", cl, 6, new IntRange(95, 100));

    // Picea concolor is a plant, but this fuzzy match is too far off
    assertMatch("Pima concolor", cl, 6, null);
    // this one should match though
    assertMatch("Pica concolor", cl, 5284657, new IntRange(85, 90));

    cl.setFamily("Pinaceae");
    assertMatch("Pima concolor", cl, 5284657, new IntRange(90, 95));


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
  public void testHomonyms2() throws IOException {
    // this hits 2 species synonyms with no such name being accepted
    // nub match must pick one if the accepted name of all synonyms is the same!
    NameUsageMatch cl = new NameUsageMatch();
    cl.setKingdom("Plantae");
    cl.setFamily("Poaceae");
    assertMatch("Elytrigia repens", cl, 2706649, new IntRange(92, 95));
  }

  @Test
  @Ignore("not implemented yet")
  public void testHybridsAndViruses() throws IOException {
    //TODO: implement
  }

  /**
   * Non existing species with old family classification should match genus Linaria.
   * http://dev.gbif.org/issues/browse/POR-2704
   */
  @Test
  public void testPOR2704() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    cl.setKingdom("Plantae");
    cl.setFamily("Scrophulariaceae"); // nowadays Plantaginaceae as in our nub/col
    assertMatch("Linaria pedunculata (L.) Chaz.", cl, 3172168, new IntRange(90,100));
  }

  /**
   * Classification names need to be parsed if they are no monomials already
   */
  @Test
  public void testClassificationWithAuthors() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    cl.setKingdom("Fungi Bartling, 1830");
    cl.setPhylum("Ascomycota Caval.-Sm., 1998");
    cl.setClazz("Lecanoromycetes, O.E. Erikss. & Winka, 1997");
    cl.setOrder("Lecanorales, Nannf., 1932");
    cl.setFamily("Ramalinaceae, C. Agardh, 1821");
    cl.setGenus("Toninia");
    assertMatch("Toninia aromatica", cl, 2608009, new IntRange(96,100));
  }

  /**
   * Non existing species should match genus Quedius
   * http://dev.gbif.org/issues/browse/POR-1712
   */
  @Test
  public void testPOR1712() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    cl.setClazz("Hexapoda");
    cl.setFamily("Staphylinidae");
    cl.setGenus("Quedius");
    cl.setKingdom("Animalia");
    cl.setPhylum("Arthropoda");
    assertMatch("Quedius caseyi divergens", cl, 4290501, new IntRange(90, 100));
  }

  /**
   * Indet subspecies should match to species Panthera pardus
   * http://dev.gbif.org/issues/browse/POR-2701
   */
  @Test
  public void testPOR2701() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    cl.setPhylum("Chordata");
    cl.setClazz("Mammalia");
    cl.setOrder("Carnivora");
    cl.setFamily("Felidae");
    cl.setGenus("Panthera");
    assertMatch("Panthera pardus ssp.", cl, 5219436, new IntRange(98, 100));
  }


  /**
   * Brunella alba Pallas ex Bieb.(Labiatae, Plantae) is wrongly matched to
   * Brunerella alba R.F. Castañeda & Vietinghoff (Fungi)
   *
   * The species does not exist in the nub and the genus Brunella is a synonym of Prunella.
   * Match to synonym genus Brunella
   * http://dev.gbif.org/issues/browse/POR-2684
   */
  @Test
  public void testPOR2684() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    cl.setKingdom("Plantae");
    cl.setFamily("Labiatae");
    cl.setGenus("Brunella");
    assertMatch("Brunella alba Pallas ex Bieb.", cl, 6008586, new IntRange(96, 100));
  }


  /**
   * The wasp species does not exist and became a spider instead.
   * Should match to the wasp genus.
   *
   * http://dev.gbif.org/issues/browse/POR-2469
   */
  @Test
  public void testPOR2469() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    cl.setKingdom("Animalia");
    cl.setPhylum("Arthropoda");
    cl.setClazz("Insecta");
    cl.setOrder("Hymenoptera");
    cl.setFamily("Tiphiidae");
    cl.setGenus("Eirone");
    assertMatch("Eirone neocaledonica Williams", cl, 4674090, new IntRange(90, 100));
  }


  /**
   * The beetle Oreina elegans does not exist in the nub and became a spider instead.
   * Should match to the wasp genus.
   *
   * http://dev.gbif.org/issues/browse/POR-2607
   */
  @Test
  public void testPOR2607() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    cl.setKingdom("Animalia");
    cl.setFamily("Chrysomelidae");
    assertMatch("Oreina", cl, 6757727, new IntRange(95, 100));
    assertMatch("Oreina elegans", cl, 6757727, new IntRange(90, 100));

    cl.setPhylum("Arthropoda");
    cl.setClazz("Insecta");
    cl.setOrder("Coleoptera");
    assertMatch("Oreina", cl, 6757727, new IntRange(98, 100));
    assertMatch("Oreina elegans", cl, 6757727, new IntRange(90, 100));
  }


  /**
   * http://gbif.blogspot.com/2015/03/improving-gbif-backbone-matching.html
   */
  @Test
  public void testBlogNames() throws IOException {
    // http://www.gbif.org/occurrence/164267402/verbatim
    LinneanClassification cl = new NameUsageMatch();
    assertMatch("Xysticus sp.", cl, 2164999, new IntRange(94, 100));
    assertMatch("Xysticus spec.", cl, 2164999, new IntRange(94, 100));

    // http://www.gbif.org/occurrence/1061576151/verbatim
    cl = new NameUsageMatch();
    cl.setFamily("Poaceae");
    cl.setGenus("Triodia");
    assertMatch("Triodia sp.", cl, 2702695, new IntRange(98, 100));

    // http://www.gbif.org/occurrence/1037140379/verbatim
    cl = new NameUsageMatch();
    cl.setKingdom("Plantae");
    cl.setFamily("XYRIDACEAE");
    cl.setGenus("Xyris");
    assertMatch("Xyris kralii Wand.", cl, 2692599, new IntRange(98, 100));

    // http://www.gbif.org/occurrence/144904719/verbatim
    cl = new NameUsageMatch();
    cl.setKingdom("Plantae");
    cl.setFamily("GRAMINEAE");
    assertMatch("Zea mays subsp. parviglumis var. huehuet Iltis & Doebley", cl, 5290052, new IntRange(98, 100));
  }

  @Test
  public void testImprovedMatching() throws IOException {
    // http://www.gbif.org/occurrence/164267402/verbatim
    LinneanClassification cl = new NameUsageMatch();
    assertMatch("Zabidius novemaculeatus", cl, 2394331, new IntRange(98, 100));
    assertNoMatch("Zabideus novemaculeatus", cl, new IntRange(65, 80));

    cl.setFamily("Ephippidae");
    assertMatch("Zabidius novemaculeatus", cl, 2394331, new IntRange(98, 100));
    assertMatch("Zabideus novemaculeatus", cl, 2394331, new IntRange(90, 98));
    assertMatch("Zabidius novaemaculeatus", cl, 2394331, new IntRange(90, 98));

    cl = new NameUsageMatch();
    cl.setKingdom("Animalia");
    cl.setFamily("Yoldiidae");
    // genus match only
    assertMatch("Yoldia bronni", cl, 2285488, new IntRange(98, 100));

    cl.setFamily("Nuculanidae");
    // genus match only
    assertMatch("Yoldia frater", cl, 2285488, new IntRange(92, 95));
  }


  /**
   * Names that ideally would fuzzy match, but do not currently
   */
  @Test
  public void testIberusGualtieranus() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    assertMatch("Iberus gualterianus minor Serradell", cl, 4406508, new IntRange(70, 100));

    cl.setFamily("Helicidae");
    assertMatch("Iberus gualterianus minor Serradell", cl, 4564258,  new IntRange(85, 98));
  }



}
