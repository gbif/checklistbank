package org.gbif.checklistbank.service.mybatis;

import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

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
}