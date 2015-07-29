package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.EstablishmentMeans;
import org.gbif.api.vocabulary.OccurrenceStatus;
import org.gbif.api.vocabulary.ThreatStatus;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DistributionServiceMyBatisIT {

  private final Integer USAGE_ID = 100000007;

  @Rule
  public DatabaseDrivenChecklistBankTestRule<DistributionService> ddt = DatabaseDrivenChecklistBankTestRule.squirrels(DistributionService.class);

  @Test
  public void testListByChecklistUsage() {
    List<Distribution> distributions = ddt.getService().listByUsage(USAGE_ID, null).getResults();
    assertEquals(4, distributions.size());

    // #15
    Distribution distribution = distributions.get(0);
    // this is a checklist record, sourceTaxonKey should only exist for nub records
    assertNull(distribution.getSourceTaxonKey());
    assertEquals("no notes", distribution.getRemarks());
    assertEquals(Country.BRAZIL, distribution.getCountry());
    assertEquals("iso:br", distribution.getLocationId());
    assertEquals("brazil", distribution.getLocality());
    assertEquals(EstablishmentMeans.INVASIVE, distribution.getEstablishmentMeans());
    assertEquals(OccurrenceStatus.PRESENT, distribution.getStatus());
    assertEquals(ThreatStatus.NEAR_THREATENED, distribution.getThreatStatus());
    assertEquals("Hecht-Markou, 1995", distribution.getSource());
    assertNull(distribution.getAppendixCites());
    assertNull(distribution.getStartDayOfYear());
    assertNull(distribution.getLifeStage());
    assertNull(distribution.getTemporal());

    for (Distribution d : distributions) {
      assertNull(d.getSourceTaxonKey());
      assertNotNull(d.getCountry());
      assertEquals(OccurrenceStatus.PRESENT, d.getStatus());
    }

    assertEquals(Country.BRAZIL, distributions.get(0).getCountry()); // #15
    assertEquals(Country.FRANCE, distributions.get(1).getCountry()); // #16
    assertEquals(Country.PERU, distributions.get(2).getCountry()); // #17
    assertEquals(Country.SURINAME, distributions.get(3).getCountry()); // #18

    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    Distribution d1 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);

    page = new PagingRequest(1, 1);
    Distribution d2 = ddt.getService().listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(d1, distributions.get(0));
    assertEquals(d2, distributions.get(1));
  }

  @Test
  public void testListByRange() {
    Map<Integer, List<Distribution>> map = ((DistributionServiceMyBatis) ddt.getService()).listRange(1, 100000020);
    assertEquals(1, map.size());

    List<Distribution> records = map.get(100000007);
    assertEquals(4, records.size());
    for (Distribution v : records) {
      assertNull(v.getSourceTaxonKey());
    }
  }
}
