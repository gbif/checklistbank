package org.gbif.nub.lookup.fuzzy;

import com.google.common.base.Joiner;
import org.apache.commons.lang.math.IntRange;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nameparser.NameParserGbifV1;
import org.gbif.nub.lookup.NubMatchingTestModule;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.IOException;

import static org.junit.Assert.*;


public class NubMatchingServiceImplIT {

  private static NubMatchingServiceImpl matcher;
  private static final Joiner CLASS_JOINER = Joiner.on("; ").useForNull("???");

  @BeforeClass
  public static void buildMatcher() throws IOException {
    matcher = new NubMatchingServiceImpl(NubMatchingTestModule.provideIndex(), NubMatchingTestModule.provideSynonyms(), new NameParserGbifV1());
  }

  private NameUsageMatch assertMatch(String name, LinneanClassification query, Integer expectedKey) {
    return assertMatch(name, query, expectedKey, null, new IntRange(1, 100));
  }

  private NameUsageMatch assertMatch(String name, LinneanClassification query, Integer expectedKey, IntRange confidence) {
    return assertMatch(name, query, expectedKey, null, confidence);
  }

  private NameUsageMatch assertMatch(String name, LinneanClassification query, Integer expectedKey, NameUsageMatch.MatchType type) {
    return assertMatch(name, query, expectedKey, type, new IntRange(1, 100));
  }

  private void print(String name, NameUsageMatch best) {
    System.out.println("\n" + name + " matches " + best.getScientificName() + " [" + best.getUsageKey() + "] with confidence " + best.getConfidence());
    if (best.getUsageKey() != null) {
      System.out.println("  " + CLASS_JOINER.join(best.getKingdom(), best.getPhylum(), best.getClazz(), best.getOrder(), best.getFamily()));
      System.out.println("  " + best.getNote());
    }
    if (best.getAlternatives() != null) {
      for (NameUsageMatch m : best.getAlternatives()) {
        System.out.println("  Alt: " + m.getScientificName() + " [" + m.getUsageKey() + "] score=" + m.getConfidence() + ". " + m.getNote());
      }
    }
  }

  private NameUsageMatch assertMatch(String name, LinneanClassification query, Integer expectedKey, @Nullable NameUsageMatch.MatchType type, IntRange confidence) {
    NameUsageMatch best = matcher.match(name, null, query, false, true);

    print(name, best);

    assertEquals("Wrong expected key", expectedKey, best.getUsageKey());
    if (type == null) {
      assertTrue("Wrong none match type", best.getMatchType() != NameUsageMatch.MatchType.NONE);
    } else {
      assertEquals("Wrong match type", type, best.getMatchType());
    }
    if (confidence != null) {
      assertTrue("confidence " + best.getConfidence() + " not within " + confidence, confidence.containsInteger(best.getConfidence()));
    }
    assertMatchConsistency(best);
    return best;
  }

  private void assertNoMatch(String name, LinneanClassification query) {
    assertNoMatch(name, query, null);
  }

  private void assertNoMatch(String name, LinneanClassification query, @Nullable IntRange confidence) {
    NameUsageMatch best = matcher.match(name, null, query, false, true);
    print(name, best);

    assertEquals(NameUsageMatch.MatchType.NONE, best.getMatchType());
    assertNull(best.getUsageKey());
  }

  static void assertMatchConsistency(NameUsageMatch match) {
    assertNotNull(match.getConfidence());
    assertNotNull(match.getMatchType());
    if (NameUsageMatch.MatchType.NONE == match.getMatchType()) {
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

      if (match.getRank() != null) {
        Rank rank = match.getRank();
        if (rank.isSuprageneric()) {
          assertNull(match.getSpecies());
          assertNull(match.getSpeciesKey());
          assertNull(match.getGenus());
          assertNull(match.getGenusKey());

        } else if (rank == Rank.GENUS) {
          assertNotNull(match.getGenus());
          assertNull(match.getSpecies());
          assertNull(match.getSpeciesKey());

        } else if (rank == Rank.SPECIES) {
          assertNotNull(match.getGenus());
          assertNotNull(match.getSpecies());
          assertNotNull(match.getSpeciesKey());
          if (!match.isSynonym()) {
            assertEquals(match.getUsageKey(), match.getSpeciesKey());
            assertTrue(match.getScientificName().startsWith(match.getSpecies()));
          }

        } else if (rank.isInfraspecific()) {
          assertNotNull(match.getGenus());
          assertNotNull(match.getSpecies());
          assertNotNull(match.getSpeciesKey());
          if (!match.isSynonym()) {
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
    assertMatch("Anephlus", cl, 1100135, new IntRange(92, 95));
    assertMatch("Aneplus", cl, 1100050, new IntRange(90, 95));

    cl.setKingdom("Animalia");
    cl.setClazz("Insecta");
    assertMatch("Aneplus", cl, 1100050, new IntRange(97, 100));

    // genus Aneplus is order=Coleoptera, but Anelus is a Spirobolida in class Diplopoda
    cl.setClazz("Diplopoda");
    cl.setOrder("Spirobolida");
    assertMatch("Aneplus", cl, 1027792, new IntRange(90, 99));

    cl.setFamily("Atopetholidae");
    assertMatch("Aneplus", cl, 1027792, new IntRange(98, 100));
    // too far off
    assertMatch("Anmeplues", cl, 1, new IntRange(90, 100));

    assertNoMatch("Anmeplues", new NameUsageMatch(), new IntRange(-10, 80));
  }

  /**
   * Sabia parviflora is a plant which is in our backbone badly classified as an animal.
   * Assure those names get matched to the next highest taxon that indeed is a plant when the plant kingdom is requested.
   */
  @Test
  public void testBadPlantKingdom() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    // without kingdom snap to the bad animal record
    assertMatch("Sabia parviflora", cl, 7268473, new IntRange(96, 100));

    // hit the plant family
    assertMatch("Sabiaceae", cl, 2409, new IntRange(90, 100));

    // make sure its the family
    cl.setKingdom("Plantae");
    cl.setFamily("Sabiaceae");
    assertMatch("Sabia parviflora", cl, 2409, new IntRange(80, 100));


    // without kingdom snap to the bad animal record
    cl = new NameUsageMatch();
    assertMatch("Tibetia tongolensis", cl, 7301567, new IntRange(96, 100));

    // hit the plant family
    assertMatch("Fabaceae", cl, 5386, new IntRange(90, 100));

    // make sure its the family
    cl.setKingdom("Plantae");
    cl.setFamily("Fabaceae");
    assertMatch("Tibetia tongolensis", cl, 5386, new IntRange(80, 100));
  }

  @Test
  public void testHomonyms() throws IOException {
    // Oenanthe
    LinneanClassification cl = new NameUsageMatch();
    assertNoMatch("Oenanthe", cl);

    cl.setKingdom("Animalia");
    assertMatch("Oenanthe", cl, 2492483, new IntRange(96, 99));

    cl.setKingdom("Plantae");
    assertMatch("Oenanthe", cl, 3034893, new IntRange(96, 99));
    assertMatch("Oenante", cl, 3034893, new IntRange(85, 95));


    // Acanthophora
    cl = new NameUsageMatch();
    assertNoMatch("Acanthophora", cl);

    // there are 3 genera in animalia, 2 synonyms and 1 accepted.
    // We prefer to match to the single accepted in this case
    cl.setKingdom("Animalia");
    assertMatch("Acanthophora", cl, 3251480, new IntRange(90, 96));

    // now try with molluscs to just get the doubtful one
    cl.setPhylum("Porifera");
    assertMatch("Acanthophora", cl, 3251480, new IntRange(97, 99));

    cl.setKingdom("Plantae"); // there are multiple plant genera, this should match to plantae now
    assertMatch("Acanthophora", cl, 6, new IntRange(95, 100));

    cl.setFamily("Araliaceae");
    assertMatch("Acanthophora", cl, 3036337, new IntRange(98, 100));
    assertMatch("Acantophora", cl, 3036337, new IntRange(90, 95)); // fuzzy match
    // try matching with authors
    assertMatch("Acantophora Merrill", cl, 3036337, new IntRange(95, 100)); // fuzzy match

    cl.setFamily("Rhodomelaceae");
    assertMatch("Acanthophora", cl, 2659277, new IntRange(97, 100));


    // species match
    cl = new NameUsageMatch();
    assertMatch("Puma concolor", cl, 2435099, new IntRange(98, 100));

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
    assertMatch("Pima concolor", cl, 6);
    // this one should match though
    assertMatch("Pica concolor", cl, 5284657, new IntRange(85, 90));
    // and this will go to the family
    cl.setFamily("Pinaceae");
    assertMatch("Pima concolor", cl, 3925, new IntRange(90, 100));


    // Amphibia is a homonym genus, but also and most prominently a class!
    cl = new NameUsageMatch();
    // non existing "species" name. Amphibia could either be the genus or the class, who knows...
    assertNoMatch("Amphibia eyecount", cl);

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
    assertMatch("Elytrigia repens", cl, 2706649, new IntRange(92, 100));
  }

  @Test
  public void testAuthorshipMatching() throws IOException {
    // this hits 2 species synonyms with no such name being accepted
    // nub match must pick one if the accepted name of all synonyms is the same!
    NameUsageMatch cl = new NameUsageMatch();
    cl.setKingdom("Plantae");
    cl.setFamily("Poaceae");
    assertMatch("Elytrigia repens", cl, 2706649, new IntRange(92, 100));
  }

  @Test
  public void testAuthorshipMatching2() throws IOException {
    NameUsageMatch cl = new NameUsageMatch();
    assertMatch("Prunella alba", cl, 5608009, new IntRange(98, 100));

    assertMatch("Prunella alba Pall. ex M.Bieb.", cl, 5608009, new IntRange(100, 100));
    assertMatch("Prunella alba M.Bieb.", cl, 5608009, new IntRange(100, 100));

    assertMatch("Prunella alba Pall.", cl, 5608009, new IntRange(80, 90));
    assertMatch("Prunella alba Döring", cl, 5608009, new IntRange(80, 90));

    // 2 homonyms exist
    assertMatch("Elytrigia repens", cl, 2706649, new IntRange(92, 98));
    assertMatch("Elytrigia repens Desv.", cl, 7522774, new IntRange(98, 100));
    assertMatch("Elytrigia repens Nevski", cl, 2706649, new IntRange(98, 100));
    assertMatch("Elytrigia repens (L.) Desv.", cl, 7522774, new IntRange(100, 100));
    assertMatch("Elytrigia repens (L.) Nevski", cl, 2706649, new IntRange(100, 100));

    // very different author, match to genus only
    assertMatch("Elytrigia repens Karimba", cl, 7826764, NameUsageMatch.MatchType.HIGHERRANK);

    // basionym author is right, now match the accepted species. Or shouldnt we?
    assertMatch("Elytrigia repens (L.) Karimba", cl, 2706649, new IntRange(80, 90));
  }

  /**
   * Testing matches of names that were different between classic species match and the author aware "lookup"
   * <p>
   * Bromus sterilis
   * Daphnia
   * Carpobrotus edulis
   * Celastrus orbiculatus
   * Python molurus subsp. bivittatus
   * Ziziphus mauritiana orthacantha
   * Solanum verbascifolium auriculatum
   */
  @Test
  public void testAuthorshipMatchingGIASIP() throws IOException {
    NameUsageMatch cl = new NameUsageMatch();
    assertMatch("Bromus sterilis", cl, 8341523, new IntRange(95, 99));
    assertMatch("Bromus sterilis Guss.", cl, 8341523, new IntRange(99, 100));
    assertMatch("Bromus sterilis Gus", cl, 8341523, new IntRange(98, 100));

    assertMatch("Bromus sterilis L.", cl, 2703647, new IntRange(98, 100));
    assertMatch("Bromus sterilis Linne", cl, 2703647, new IntRange(98, 100));

    assertMatch("Bromus sterilis Kumm. & Sendtn.", cl, 7874095, new IntRange(98, 100));
    assertMatch("Bromus sterilis Kumm", cl, 7874095, new IntRange(98, 100));
    assertMatch("Bromus sterilis Sendtn.", cl, 7874095, new IntRange(98, 100));


    assertMatch("Daphnia", cl, 2234785, new IntRange(90, 95));
    assertMatch("Daphnia Müller, 1785", cl, 2234785, new IntRange(96, 100));
    assertMatch("Daphnia Müller", cl, 2234785, new IntRange(94, 98));

    assertMatch("Daphne Müller, 1785", cl, 2234879, new IntRange(95, 98));
    assertMatch("Daphne Müller, 1776", cl, 2234879, new IntRange(96, 100));

    assertMatch("Daphnia Korth", cl, 7956551, new IntRange(86, 94));
    cl.setKingdom("Plantae");
    cl.setFamily("Oxalidaceae");
    assertMatch("Daphnia Korth", cl, 3626852, new IntRange(96, 100));
    cl = new NameUsageMatch();

    assertMatch("Daphnia Rafinesque", cl, 7956551, new IntRange(88, 94));
    cl.setKingdom("Animalia");
    cl.setFamily("Calanidae");
    assertMatch("Daphnia Rafinesque", cl, 4333792, new IntRange(98, 100));
    cl = new NameUsageMatch();


    assertMatch("Carpobrotus edulis", cl, 3084842, new IntRange(95, 99));
    assertMatch("Carpobrotus edulis N. E. Br.", cl, 3084842, new IntRange(98, 100));
    assertMatch("Carpobrotus edulis L.Bolus", cl, 7475472, new IntRange(95, 100));
    assertMatch("Carpobrotus edulis Bolus", cl, 7475472, new IntRange(95, 100));
    assertMatch("Carpobrotus dulcis Bolus", cl, 3703510, new IntRange(95, 100));
    // once again with classification given
    cl.setKingdom("Plantae");
    cl.setFamily("Celastraceae");
    assertMatch("Carpobrotus edulis", cl, 3084842, new IntRange(92, 98));
    assertMatch("Carpobrotus edulis N. E. Br.", cl, 3084842, new IntRange(98, 100));
    assertMatch("Carpobrotus edulis L.Bolus", cl, 7475472, new IntRange(95, 100));
    assertMatch("Carpobrotus edulis Bolus", cl, 7475472, new IntRange(95, 100));
    assertMatch("Carpobrotus dulcis Bolus", cl, 3703510, new IntRange(95, 100));
    cl = new NameUsageMatch();


    assertMatch("Celastrus orbiculatus", cl, 8104460, new IntRange(95, 99));
    assertMatch("Celastrus orbiculatus Murray", cl, 8104460, new IntRange(98, 100));
    assertMatch("Celastrus orbiculatus Thunb", cl, 3169169, new IntRange(98, 100));
    assertMatch("Celastrus orbiculatus Lam", cl, 7884995, new IntRange(98, 100));


    assertMatch("Python molurus subsp. bivittatus", cl, 6162891, new IntRange(98, 100));
    assertMatch("Python molurus bivittatus", cl, 6162891, new IntRange(98, 100));
    assertMatch("Python molurus bivittatus Kuhl", cl, 6162891, new IntRange(98, 100));
    assertMatch("Python molurus subsp. bibittatus", cl, 4287608, new IntRange(97, 100));


    assertMatch("Ziziphus mauritiana orthacantha", cl, 7786586, new IntRange(95, 98));
    assertMatch("Ziziphus mauritiana ssp. orthacantha", cl, 7786586, new IntRange(97, 100));
    assertMatch("Ziziphus mauritiana ssp. orthacantha Chev.", cl, 7786586, new IntRange(98, 100));
    assertMatch("Ziziphus mauritiana var. orthacantha", cl, 8068734, new IntRange(97, 100));
    assertMatch("Ziziphus mauritiana var. orthacantha Chev.", cl, 8068734, new IntRange(98, 100));


    assertMatch("Solanum verbascifolium auriculatum", cl, 2930718, new IntRange(95, 98));
    assertMatch("Solanum verbascifolium ssp. auriculatum", cl, 2930718, new IntRange(95, 99));
    assertMatch("Solanum verbascifolium var. auriculatum Kuntze", cl, 8363606, new IntRange(98, 100));
    assertMatch("Solanum verbascifolium var. auriculatum Maiden", cl, 6290014, new IntRange(98, 100));
    assertMatch("Solanum verbascifolium var. auriculatum", cl, 6290014, new IntRange(94, 98));
  }

  @Test
  @Ignore("not implemented yet")
  public void testHybrids() throws IOException {
    //TODO: implement
  }

  @Test
  public void testOtuMatching() throws IOException {
    NameUsageMatch cl = new NameUsageMatch();
    NameUsageMatch m = assertMatch("BOLD:AAX3687", cl, 993172099, new IntRange(90, 100));
    assertEquals("BOLD:AAX3687", m.getScientificName());

    assertMatch("SH021315.07FU", cl, 993730906, new IntRange(90, 100));

    cl.setFamily("Maldanidae");
    assertMatch("bold:aax3687", cl, 993172099, new IntRange(95, 100));

    assertNoMatch("BOLD:AAX3688", cl);
    assertNoMatch("BOLD:AAY3687", cl);
    assertNoMatch("COLD:AAX3687", cl);
    assertNoMatch("AAX3687", cl);
  }

  /**
   * http://dev.gbif.org/issues/browse/PF-2574
   * <p>
   * Inachis io (Linnaeus, 1758)
   * Inachis io NPV
   * <p>
   * Dionychopus amasis GV
   * <p>
   * Hyloicus pinastri NPV
   * Hylobius pinastri Billberg
   * Hylobius (Hylobius) pinastri Escherich , 1923
   * <p>
   * Vanessa cardui NPV
   * Vanessa cardui (Linnaeus, 1758)
   */
  @Test
  public void testViruses() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    assertMatch("Inachis io", cl, 5881450, new IntRange(92, 100));
    assertMatch("Inachis io (Linnaeus)", cl, 5881450, new IntRange(95, 100));
    assertMatch("Inachis io NPV", cl, 8562651, new IntRange(90, 98));

    assertMatch("Dionychopus amasis GV", cl, 6876449, new IntRange(90, 98));
    // to lepidoptera genus only
    assertMatch("Dionychopus amasis", cl, 4689754, new IntRange(90, 100));
    // with given kingdom result in no match, GV is part of the name
    cl.setKingdom("Virus");
    assertNoMatch("Dionychopus amasis", cl);
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
    assertMatch("Linaria pedunculata (L.) Chaz.", cl, 3172168, new IntRange(90, 100));
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
    assertMatch("Toninia aromatica", cl, 2608009, new IntRange(96, 100));
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
   * <p>
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
   * <p>
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
   * Allow lower case names
   * <p>
   * https://github.com/gbif/portal-feedback/issues/1379
   */
  @Test
  public void testFeedback1379() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    cl.setFamily("Helicidae");
    assertMatch("iberus gualterianus", cl, 4564258, new IntRange(90, 99));
  }


  /**
   * The beetle Oreina elegans does not exist in the nub and became a spider instead.
   * Should match to the wasp genus.
   * <p>
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
    assertMatch("Oreina elegans", cl, 6757727, NameUsageMatch.MatchType.HIGHERRANK);
  }


  /**
   * http://gbif.blogspot.com/2015/03/improving-gbif-backbone-matching.html
   */
  @Test
  public void testBlogNames() throws IOException {
    // http://www.gbif.org/occurrence/164267402/verbatim
    LinneanClassification cl = new NameUsageMatch();
    assertMatch("Xysticus sp.", cl, 2164999, NameUsageMatch.MatchType.HIGHERRANK);
    assertMatch("Xysticus spec.", cl, 2164999, NameUsageMatch.MatchType.HIGHERRANK);

    // http://www.gbif.org/occurrence/1061576151/verbatim
    cl = new NameUsageMatch();
    cl.setFamily("Poaceae");
    cl.setGenus("Triodia");
    assertMatch("Triodia sp.", cl, 2702695);

    // http://www.gbif.org/occurrence/1037140379/verbatim
    cl = new NameUsageMatch();
    cl.setKingdom("Plantae");
    cl.setFamily("XYRIDACEAE");
    cl.setGenus("Xyris");

    // only to the genus 2692599
    // Xyris jolyi Wand. & Cerati 2692999
    assertMatch("Xyris kralii Wand.", cl, 2692599, NameUsageMatch.MatchType.HIGHERRANK);

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
    assertMatch("Zabidius novaemaculeatus", cl, 2394331, new IntRange(90, 100));
    assertMatch("Zabidius novaemaculeata", cl, 2394331, new IntRange(90, 100));
    // no name normalization on the genus, but a fuzzy match
    assertMatch("Zabideus novemaculeatus", cl, 2394331, NameUsageMatch.MatchType.FUZZY, new IntRange(85, 95));

    cl = new NameUsageMatch();
    cl.setKingdom("Animalia");
    cl.setFamily("Yoldiidae");
    // genus match only
    assertMatch("Yoldia bronni", cl, 2285488, new IntRange(98, 100));

    cl.setFamily("Nuculanidae");
    // genus match only
    assertMatch("Yoldia frate", cl, 2285488, new IntRange(90, 95));
  }

  /**
   * Names that fuzzy match to higher species "Iberus gualtieranus"
   */
  @Test
  public void testIberusGualtieranus() throws IOException {
    LinneanClassification cl = new NameUsageMatch();
    assertMatch("Iberus gualterianus minor Serradell", cl, 4564258, new IntRange(90, 99));

    cl.setFamily("Helicidae");
    assertMatch("Iberus gualterianus minor Serradell", cl, 4564258, new IntRange(90, 99));
  }


}
