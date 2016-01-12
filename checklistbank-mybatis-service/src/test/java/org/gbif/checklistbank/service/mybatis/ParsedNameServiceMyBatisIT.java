package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.nameparser.NameParser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ParsedNameServiceMyBatisIT extends MyBatisServiceITBase<ParsedNameService> {

  public ParsedNameServiceMyBatisIT() {
    super(ParsedNameService.class);
  }

  @Test
  public void testCreateOrGet() throws Exception {
    ParsedName pn = new ParsedName();
    pn.setScientificName("Abies alba Mill.");
    pn.setGenusOrAbove("Abies");
    pn.setAuthorship("Mill.");
    pn.setSpecificEpithet("alba");
    pn.setType(NameType.SCIENTIFIC);
    assertNull(pn.getKey());

    ParsedName pn2 = service.createOrGet(pn);
    assertNotNull(pn2.getKey());
    assertEquals("Abies alba Mill.", pn2.getScientificName());
    assertEquals("Abies alba", pn2.canonicalName());
    assertEquals("Abies", pn2.getGenusOrAbove());
    assertEquals("alba", pn2.getSpecificEpithet());
    assertEquals("Mill.", pn2.getAuthorship());

    final NameParser parser = new NameParser();
    pn = service.createOrGet(parser.parse("Abies alba Mill.", null));
    assertEquals("Abies alba Mill.", pn.getScientificName());
    assertEquals("Abies alba", pn.canonicalName());
    assertEquals("Abies", pn.getGenusOrAbove());
    assertEquals("alba", pn.getSpecificEpithet());
    assertEquals("Mill.", pn.getAuthorship());

    pn = service.createOrGet(parser.parse("Abies sp.", null));
    assertEquals("Abies sp.", pn.getScientificName());
    assertEquals("Abies spec.", pn.canonicalName());
    assertEquals("Abies", pn.getGenusOrAbove());
    assertEquals("sp.", pn.getRankMarker());
    assertEquals(Rank.SPECIES, pn.getRank());
    assertNull(pn.getSpecificEpithet());

    pn = service.createOrGet(parser.parse("×Abies Mill.", null));
    assertEquals("×Abies Mill.", pn.getScientificName());
    assertEquals("Abies", pn.canonicalName());
    assertEquals("Abies", pn.getGenusOrAbove());
    assertNull(pn.getRankMarker());
    assertNull(pn.getRank());
    assertNull(pn.getSpecificEpithet());
    assertEquals(NamePart.GENERIC, pn.getNotho());

    pn = service.createOrGet(parser.parse("? hostilis Gravenhorst, 1829", null));
    assertEquals("? hostilis Gravenhorst, 1829", pn.getScientificName());
    assertNull(pn.canonicalName());
    assertNull(pn.getGenusOrAbove());
    assertNull(pn.getRankMarker());
    assertNull(pn.getRank());
    assertNull(pn.getSpecificEpithet());
  }

  @Test
  public void testReparse() throws Exception {
    assertEquals(1150, service.reparseAll());
  }

  @Test
  public void testOrphaned() throws Exception {
    assertEquals(1107, ((ParsedNameServiceMyBatis) service).deleteOrphaned());
  }

}