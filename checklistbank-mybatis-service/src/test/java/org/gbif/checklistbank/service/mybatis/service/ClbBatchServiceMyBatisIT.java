package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule2;

import java.net.URI;
import java.util.List;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class ClbBatchServiceMyBatisIT extends MyBatisServiceITBase {

  private final UsageService service;

  @Autowired
  public ClbBatchServiceMyBatisIT(DataSource dataSource, UsageService usageService) {
    super(dataSource);
    this.service = usageService;
  }

  @Test
  public void testListAll() {
    List<Integer> ids = service.listAll();
    assertEquals(46, ids.size());
  }

  @Test
  public void testListRange() {
    List<NameUsage> usages = service.listRange(100000001, 100000020);
    assertEquals(20, usages.size());

    boolean found = false;
    for (NameUsage nu : usages) {
      assertNull(nu.getVernacularName());
      assertNotNull(nu.getNameKey());
      assertNotNull(nu.getScientificName());
      assertTrue(nu.getKey() >= 100000001 && nu.getKey() <= 100000020);
      assertEquals(ClbDbTestRule2.SQUIRRELS_DATASET_KEY, nu.getDatasetKey());

      if (nu.getKey().equals(100000007)) {
        found = true;
        assertEquals("6905528", nu.getTaxonID());
        assertEquals(
            URI.create("http://www.catalogueoflife.org/details/species/id/6905528"),
            nu.getReferences());
      }
    }
    if (!found) {
      fail("usage 100000007 missing in range result");
    }
  }
}
