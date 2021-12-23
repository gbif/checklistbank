package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.SpeciesProfileService;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SpeciesProfileServiceMyBatisIT extends MyBatisServiceITBase2 {

  private final SpeciesProfileService service;

  private static final Integer USAGE_ID = 100000025;

  @Autowired
  public SpeciesProfileServiceMyBatisIT(
      DataSource dataSource, SpeciesProfileService speciesProfileService) {
    super(dataSource);
    this.service = speciesProfileService;
  }

  public void verify1(SpeciesProfile sp) {
    assertNull(sp.getSourceTaxonKey());
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
    List<SpeciesProfile> sps = service.listByUsage(USAGE_ID, null).getResults();
    assertEquals(2, sps.size());
    assertEquals("boreal forest", sps.get(0).getHabitat());
    assertEquals("temperate", sps.get(1).getHabitat());
    verify1(sps.get(0));
  }

  @Test
  public void testListByChecklistUsagePaging() {
    List<SpeciesProfile> sps = service.listByUsage(USAGE_ID, null).getResults();
    Pageable page = new PagingRequest(0, 1);
    SpeciesProfile sp1 = service.listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(sp1, sps.get(0));
    page = new PagingRequest(1, 1);
    SpeciesProfile sp2 = service.listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(sp2, sps.get(1));
  }

  @Test
  public void testListByRange() {
    Map<Integer, List<SpeciesProfile>> map =
        ((SpeciesProfileServiceMyBatis) service).listRange(1, 100000130);
    assertEquals(2, map.size());

    List<SpeciesProfile> records = map.get(100000025);
    assertEquals(2, records.size());
    for (SpeciesProfile v : records) {
      assertNull(v.getSourceTaxonKey());
    }
  }
}
