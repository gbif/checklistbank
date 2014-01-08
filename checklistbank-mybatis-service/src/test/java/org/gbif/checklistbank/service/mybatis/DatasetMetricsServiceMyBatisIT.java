package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.service.checklistbank.DatasetMetricsService;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.util.List;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatasetMetricsServiceMyBatisIT {

  private static final UUID CHECKLIST_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");

  @Rule
  public DatabaseDrivenChecklistBankTestRule<DatasetMetricsService> ddt =
    new DatabaseDrivenChecklistBankTestRule<DatasetMetricsService>(DatasetMetricsService.class);

  @Test
  public void testGet() {
    DatasetMetrics d = ddt.getService().get(CHECKLIST_KEY);
    assertEquals(CHECKLIST_KEY, d.getDatasetKey());
    assertEquals(1000, d.getUsagesCount());
    assertEquals(25, d.getColCoveragePct());
    assertEquals(250, d.getColMatchingCount());
    assertEquals(100, d.getCountByKingdom(Kingdom.ANIMALIA));
    assertEquals(700, d.getCountByKingdom(Kingdom.PLANTAE));
    assertEquals(0, d.getCountByKingdom(Kingdom.FUNGI));
    assertEquals(120, d.getCountByRank(Rank.GENUS));
    assertEquals(10, d.getCountByRank(Rank.PHYLUM));
    assertEquals(4, d.getCountNamesByLanguage(Language.DANISH));
    assertEquals(132, d.getCountNamesByLanguage(Language.GERMAN));
  }

  @Test
  public void testList() {
    List<DatasetMetrics> ds = ddt.getService().list(CHECKLIST_KEY);
    assertEquals(3, ds.size());
    for (DatasetMetrics d : ds) {
      assertEquals(CHECKLIST_KEY, d.getDatasetKey());
    }
    assertEquals(1000, ds.get(0).getUsagesCount());
    assertEquals(200, ds.get(1).getUsagesCount());
    assertEquals(100, ds.get(2).getUsagesCount());
  }

}
