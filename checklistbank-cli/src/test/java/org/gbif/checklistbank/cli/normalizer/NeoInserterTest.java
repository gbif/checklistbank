package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.dwc.terms.DwcTerm;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class NeoInserterTest {
  NeoInserter ins = new NeoInserter();

  @Test
  public void testSetScientificName() throws Exception {

    VerbatimNameUsage v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.scientificName, "Abies alba Mill., 1982");
    assertName(v, Rank.SPECIES, "Abies alba Mill., 1982", "Abies alba", NameType.WELLFORMED);

    v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.scientificName, "Abies alba");
    assertName(v, Rank.SPECIES, "Abies alba", "Abies alba", NameType.WELLFORMED);

    v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.scientificName, "? alba");
    assertName(v, Rank.SPECIES, "? alba", null, NameType.DOUBTFUL);

    v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.genus, "Abies");
    v.setCoreField(DwcTerm.specificEpithet, "alba");
    assertName(v, Rank.SPECIES, "Abies alba", "Abies alba", NameType.WELLFORMED);

    v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.genus, "Abies");
    v.setCoreField(DwcTerm.specificEpithet, "alba");
    v.setCoreField(DwcTerm.infraspecificEpithet, "alpina");
    assertName(v, Rank.SUBSPECIES, "Abies alba subsp. alpina", "Abies alba alpina", NameType.WELLFORMED);

    v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.genus, "Abies");
    v.setCoreField(DwcTerm.infraspecificEpithet, "alpina");
    assertName(v, Rank.SUBSPECIES, "Abies subsp.", "Abies subsp.", NameType.WELLFORMED);
  }

  @Test(expected = IgnoreNameUsageException.class)
  public void testSetScientificNameExc() throws Exception {
    VerbatimNameUsage v = new VerbatimNameUsage();
    assertName(v, Rank.SPECIES, null, null, NameType.BLACKLISTED);
  }

  private NameUsageContainer assertName(VerbatimNameUsage v, Rank rank, String sciname, String canonical, NameType ntype)
    throws IgnoreNameUsageException {
    NameUsageContainer u = new NameUsageContainer();
    ins.setScientificName(u, v, rank);
    if (sciname != null) {
      assertEquals(sciname, u.getScientificName());
    } else {
      assertNull(u.getScientificName());
    }
    if (canonical != null) {
      assertEquals(canonical, u.getCanonicalName());
    } else {
      assertNull(u.getCanonicalName());
    }
    if (ntype != null) {
      assertEquals(ntype, u.getNameType());
    } else {
      assertNull(u.getNameType());
    }
    return u;
  }

  @Test
  public void testClean() throws Exception {
    assertNull(NeoInserter.clean(null));
    assertNull(NeoInserter.clean(" "));
    assertNull(NeoInserter.clean("   "));
    assertNull(NeoInserter.clean("\\N"));
    assertNull(NeoInserter.clean("NULL"));
    assertNull(NeoInserter.clean("\t "));

    assertEquals("Abies", NeoInserter.clean("Abies"));
    assertEquals("öAbies", NeoInserter.clean("öAbies"));
    assertEquals("Abies  mille", NeoInserter.clean(" Abies  mille"));
  }
}