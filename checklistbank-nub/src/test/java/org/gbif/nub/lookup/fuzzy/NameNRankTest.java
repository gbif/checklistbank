package org.gbif.nub.lookup.fuzzy;

import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.model.Classification;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.*;

class NameNRankTest {

  @Test
  void build() {
    assertName("Asteraceae Mill.",
        "Asteraceae", "Mill.");
    assertName("Lepidothrix iris L.",
        "Lepidothrix iris", "L.", "iris", "", null
    );

    LinneanClassification cl = new Classification();
    cl.setKingdom("Animalia");
    cl.setPhylum("Chordata");
    cl.setClazz("Aves");
    cl.setOrder("Passeriformes");
    cl.setFamily("Pipridae");
    cl.setGenus("Lepidothrix");

    assertName("Lepidothrix iris L.",
        "", "L.", null, "iris", "", cl
    );
    assertName("Lepidothrix iris L.",
        "L. iris", "L.", null, "", "null",  cl
    );
    assertName("Lepidothrix iris L.",
        "L. iris", "L.", "Lepidothrix", "", null
    );
    assertName("L. iris",
        "L. iris", "L.", null, "", null,  null
    );
    assertName("L. iris Miller",
        "L. iris", "Miller", null, "", null,  null
    );

    cl.setSpecies("iris");
    assertName("Lepidothrix iris L.",
        "iris", "L.", null, "", null,  cl
    );
    cl.setSpecies("Lepidothrix iris");
    assertName("Lepidothrix iris L.",
        null, "L.", null, "", null,  cl
    );

    // without classification
    assertName("Lepidothrix iris L.",
        null, "L.", "Lepidothrix", "iris", null,  cl
    );
    assertName("Lepidothrix iris var. alpina L.", Rank.VARIETY,
        null, "L.", "Lepidothrix", "iris", Rank.VARIETY, "alpina",  cl
    );

    assertGoodName("Aa calceata (Rchb.f.) Schltr.", null);
    assertGoodName("Aa calceata (Rchb.f.) Schltr.", "(Rchb.f.) Schltr.");
    assertGoodName("Aa calceata (Rchb.f.) Schltr.", "Schltr.");
    assertGoodName("Deltalipothrixvirus SBFV3", null);
    assertGoodName("SH002390.07FU", null);
    assertGoodName("Acidianus filamentous virus 7", null);
    assertGoodName("Lepidothrix iris L.", null);
    assertGoodName("Polygola vulgaris var. alpina DC.", null);
    assertGoodName("Polygola vulgaris var. alpina DC.", "DC.");
  }

  void assertName(String expected, String name, String authorship) {
    assertName(expected, null, name, authorship);
  }
  void assertName(String expected, Rank rank, String name, String authorship) {
    assertName(expected, rank, name, authorship,null,  null, null);
  }
  void assertName(String expected, String name, String authorship, String genericName, String specificEpithet, String infraSpecificEpithet) {
    assertName(expected, null, name, authorship, genericName, specificEpithet, infraSpecificEpithet);
  }
  void assertName(String expected, Rank rank, String name, String authorship, String genericName, String specificEpithet, String infraSpecificEpithet) {
    assertName(expected, rank, name, authorship, null, genericName, specificEpithet, infraSpecificEpithet, null);
  }
  void assertName(String expected, String name, String authorship, String genericName, String specificEpithet, String infraSpecificEpithet, LinneanClassification classification) {
    assertName(expected, null, name, authorship, null, genericName, specificEpithet, infraSpecificEpithet, classification);
  }
  void assertName(String expected, String name, String authorship, String genericName, String specificEpithet, Rank rank, String infraSpecificEpithet, LinneanClassification classification) {
    assertName(expected, null, name, authorship, rank, genericName, specificEpithet, infraSpecificEpithet, classification);
  }
  void assertName(String expected, Rank expectedRank, String name, String authorship, String genericName, String specificEpithet, Rank rank, String infraSpecificEpithet, LinneanClassification classification) {
    assertName(expected, expectedRank, name, authorship, rank, genericName, specificEpithet, infraSpecificEpithet, classification);
  }
  void assertName(String expected, Rank expectedRank, String name, String authorship, Rank rank, String genericName, String specificEpithet, String infraSpecificEpithet, LinneanClassification classification) {
    assertEquals(new NameNRank(expected, expectedRank), NameNRank.build(
        name, authorship, genericName, specificEpithet, infraSpecificEpithet, rank,  classification)
    );
  }
  void assertGoodName(String name, String authorship) {
    assertEquals(new NameNRank(name, null), NameNRank.build(
        name, authorship, null, null, null, null,  null)
    );
  }

  @Test
  void appendAuthorship() {
    assertEquals("Abies Mill.", NameNRank.appendAuthorship("Abies", "Mill."));
    assertEquals("Abies Mill.", NameNRank.appendAuthorship("Abies ", "Mill. "));
    assertEquals("Abies Mill.", NameNRank.appendAuthorship("Abies Mill.", "Mill."));
    assertNull(NameNRank.appendAuthorship(null, "Mill."));
    assertNull(NameNRank.appendAuthorship(" ", "Mill."));
  }

  @Test
  void expandAbbreviatedGenus() {
    assertEquals("Abies Mill.", NameNRank.expandAbbreviatedGenus("Abies Mill.", ""));
    assertEquals("Abies alba Mill.", NameNRank.expandAbbreviatedGenus("Abies alba Mill.", "Abies"));
    assertEquals("Abies alba Mill.", NameNRank.expandAbbreviatedGenus("A alba Mill.", "Abies"));
    assertEquals("Abies alba Mill.", NameNRank.expandAbbreviatedGenus("A. alba Mill.", "Abies"));
    assertEquals("Abies alba Mill.", NameNRank.expandAbbreviatedGenus("? alba Mill.", "Abies"));
    assertEquals("xecrtvfzgbhnjml,mtre", NameNRank.expandAbbreviatedGenus("xecrtvfzgbhnjml,mtre", "Abies"));
    assertEquals("FU234567", NameNRank.expandAbbreviatedGenus("FU234567", "Abies"));
  }

  @Test
  void isSimpleBinomial() {
    assertTrue(NameNRank.isSimpleBinomial("Abies alba"));
    assertTrue(NameNRank.isSimpleBinomial("Abies  alba "));
    assertTrue(NameNRank.isSimpleBinomial("Adalia bipunctata"));
    assertTrue(NameNRank.isSimpleBinomial("Adalia bi-punctata"));
    assertTrue(NameNRank.isSimpleBinomial("Adalia 8-punctata"));
    assertTrue(NameNRank.isSimpleBinomial("Adalia 8punctata"));

    assertFalse(NameNRank.isSimpleBinomial("Abies alba Mill."));
    assertFalse(NameNRank.isSimpleBinomial("alba Mill."));
    assertFalse(NameNRank.isSimpleBinomial("alba"));
    assertFalse(NameNRank.isSimpleBinomial("Abies"));
  }
}