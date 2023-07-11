package org.gbif.nub.lookup.fuzzy;

import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.model.Classification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameNRankTest {

  @Test
  void build() {
    LinneanClassification cl = new Classification();
    assertEquals(new NameNRank("Asteraceae Mill.", Rank.FAMILY), NameNRank.build(
        "Asteraceae", "Mill.", null, "", Rank.FAMILY,  cl)
    );
    assertEquals(new NameNRank("Lepidothrix iris L.", null), NameNRank.build(
        "Lepidothrix iris", "L.", "iris", "", null,  cl)
    );

    cl.setGenus("Lepidothrix");
    assertEquals(new NameNRank("Lepidothrix iris L.", null), NameNRank.build(
        "", "L.", "iris", "", null,  cl)
    );
    assertEquals(new NameNRank("Lepidothrix iris L.", null), NameNRank.build(
        "L. iris", "L.", null, "", null,  cl)
    );
    assertEquals(new NameNRank("L. iris", null), NameNRank.build(
        "L. iris", "L.", null, "", null,  null)
    );
    assertEquals(new NameNRank("L. iris Miller", null), NameNRank.build(
        "L. iris", "Miller", null, "", null,  null)
    );

    cl.setSpecies("iris");
    assertEquals(new NameNRank("Lepidothrix iris L.", null), NameNRank.build(
        "iris", "L.", null, "", null,  cl)
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

  void assertGoodName(String name, String authorship) {
    assertEquals(new NameNRank(name, null), NameNRank.build(
        name, authorship, null, null, null,  null)
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