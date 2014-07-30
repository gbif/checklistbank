package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.model.RawUsage;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Mappers are not exposed from the guice module but are internal only.
 * The name usage service therefore has simple delegate methods so we can test them with our IT test framework.
 */
public class RawUsageMapperIT {

  private static final UUID CHECKLIST_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");

  @Rule
  public DatabaseDrivenChecklistBankTestRule<NameUsageService> ddt =
    new DatabaseDrivenChecklistBankTestRule<NameUsageService>(NameUsageService.class);


  @Test
  public void insertRaw() {
    final int key = 100000001;
    final byte[] data = "xeghwax542tgld@".getBytes();
    RawUsage raw = new RawUsage();
    raw.setUsageKey(key);
    raw.setDatasetKey(CHECKLIST_KEY);
    // date is null in dataset_metrics table
    //raw.setLastCrawled(new Date());
    raw.setData(data);

    ((NameUsageServiceMyBatis) ddt.getService()).insertRaw(raw);

    RawUsage raw2 = ((NameUsageServiceMyBatis) ddt.getService()).getRaw(key);
    Assert.assertEquals(raw, raw2);
  }


  @Test
  public void updateRaw() {
    final int key = 100000001;
    final byte[] data = "xeghwax542tgld@".getBytes();

    RawUsage raw = new RawUsage();
    raw.setUsageKey(key);
    raw.setDatasetKey(CHECKLIST_KEY);
    ((NameUsageServiceMyBatis) ddt.getService()).insertRaw(raw);

    // date is null in dataset_metrics table
    //raw.setLastCrawled(new Date());
    raw.setData(data);
    ((NameUsageServiceMyBatis) ddt.getService()).updateRaw(raw);

    RawUsage raw2 = ((NameUsageServiceMyBatis) ddt.getService()).getRaw(key);
    Assert.assertEquals(raw, raw2);
  }

}