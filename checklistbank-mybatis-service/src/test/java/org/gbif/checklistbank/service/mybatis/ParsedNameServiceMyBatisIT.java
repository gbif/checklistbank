package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class ParsedNameServiceMyBatisIT {

  private static final UUID CHECKLIST_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");

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
  }
}