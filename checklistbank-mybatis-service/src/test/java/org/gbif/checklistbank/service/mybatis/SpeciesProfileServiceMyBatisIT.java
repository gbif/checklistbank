package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SpeciesProfileServiceMyBatisIT {

  private static final Integer USAGE_ID = 100000025;

  @Rule
  public DatabaseDrivenChecklistBankTestRule<SpeciesProfileService> ddt =
    new DatabaseDrivenChecklistBankTestRule<SpeciesProfileService>(SpeciesProfileService.class);

  @Test
  public void testGet() {
    SpeciesProfile sp = ddt.getService().get(1);
    assertEquals(USAGE_ID, sp.getUsageKey());
    assertEquals(3650, sp.getAgeInDays().intValue());
    assertEquals("boreal forest", sp.getHabitat());
    assertEquals(null, sp.getLifeForm());
    assertEquals("Pleistocene to Present", sp.getLivingPeriod());
    assertEquals(340, sp.getMassInGram().intValue());
    assertEquals(430, sp.getSizeInMillimeter().intValue());
    assertFalse(sp.isExtinct());
    assertFalse(sp.isHybrid());
    assertFalse(sp.isMarine());
    assertTrue(sp.isTerrestrial());
  }

  @Test
  public void testListByUsage() {
    List<SpeciesProfile> sps = ddt.getService().listByUsage(USAGE_ID, null).getResults();
    assertEquals(2, sps.size());
    assertEquals("boreal forest", sps.get(0).getHabitat());
    assertEquals("temperate", sps.get(1).getHabitat());
  }

  @Test
  public void testListByChecklistUsagePaging() {
    List<SpeciesProfile> sps = ddt.getService().listByUsage(USAGE_ID, null).getResults();
    Pageable page = new PagingRequest(0, 1);
    SpeciesProfile sp1 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(sp1, sps.get(0));
    page = new PagingRequest(1, 1);
    SpeciesProfile sp2 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(sp2, sps.get(1));
  }

  @Test
  public void testListByRange() {
    List<SpeciesProfile> records = ((SpeciesProfileServiceMyBatis) ddt.getService()).listRange(1, 100000020);
    assertEquals(0, records.size());
    for (SpeciesProfile v : records) {
      assertNotNull(v.getUsageKey());
    }
  }
}

