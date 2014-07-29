package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;
import org.gbif.dwc.terms.DwcTerm;

import java.util.Date;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

public class DatasetImportServiceMyBatisIT {
  private static final UUID CHECKLIST_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");

  @Rule
  public DatabaseDrivenChecklistBankTestRule<DatasetImportService> ddt =
    new DatabaseDrivenChecklistBankTestRule<DatasetImportService>(DatasetImportService.class);


  @Test
  public void testDeleteDataset() throws Exception {
    ddt.getService().deleteDataset(CHECKLIST_KEY);
  }

  @Test
  public void testDeleteOldUsages() throws Exception {
    ddt.getService().deleteOldUsages(CHECKLIST_KEY, new Date());
  }

  @Test
  public void testSyncUsage() throws Exception {
    final int key = 7896;
    final String taxonID = "gfzd8";
    String name = "Abies alba Mill.";

    NameUsageContainer u = new NameUsageContainer();
    u.setKey(key);
    u.setScientificName(name);
    u.setTaxonID(taxonID);

    NameUsageMetrics m = new NameUsageMetrics();
    m.setKey(key);
    m.setNumSpecies(1);
    m.setNumDescendants(4);
    m.setNumSynonyms(3);

    ddt.getService().syncUsage(CHECKLIST_KEY, u, null, m);

    // with verbatim data
    VerbatimNameUsage v = new VerbatimNameUsage();
    v.setKey(key);
    v.setCoreField(DwcTerm.scientificName, name);
    v.setCoreField(DwcTerm.taxonID, taxonID);

    ddt.getService().syncUsage(CHECKLIST_KEY, u, v, m);
  }
}