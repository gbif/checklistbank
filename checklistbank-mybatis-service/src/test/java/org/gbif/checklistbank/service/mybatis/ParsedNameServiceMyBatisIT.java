package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class ParsedNameServiceMyBatisIT {

  @Rule
  public DatabaseDrivenChecklistBankTestRule<ParsedNameService> ddt =
    new DatabaseDrivenChecklistBankTestRule<ParsedNameService>(ParsedNameService.class);

  @Test
  public void testCreateOrGet() throws Exception {
    ParsedName pn = ddt.getService().createOrGet("Abies alba Mill.");
    Assert.assertEquals("Abies alba Mill.", pn.getScientificName());
    Assert.assertEquals("Abies alba", pn.canonicalName());
    Assert.assertEquals("Abies", pn.getGenusOrAbove());
    Assert.assertEquals("alba", pn.getSpecificEpithet());
    Assert.assertEquals("Mill.", pn.getAuthorship());

    pn = ddt.getService().createOrGet("Abies sp.");
    Assert.assertEquals("Abies sp.", pn.getScientificName());
    Assert.assertEquals("Abies spec.", pn.canonicalName());
    Assert.assertEquals("Abies", pn.getGenusOrAbove());
    Assert.assertEquals("sp.", pn.getRankMarker());
    Assert.assertEquals(Rank.SPECIES, pn.getRank());
    Assert.assertNull(pn.getSpecificEpithet());
  }
}