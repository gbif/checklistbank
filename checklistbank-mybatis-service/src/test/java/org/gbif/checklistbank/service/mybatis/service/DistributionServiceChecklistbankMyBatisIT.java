/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DistributionStatus;
import org.gbif.api.vocabulary.EstablishmentMeans;
import org.gbif.api.vocabulary.ThreatStatus;
import org.gbif.ChecklistbankMyBatisServiceITBase;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DistributionServiceChecklistbankMyBatisIT extends ChecklistbankMyBatisServiceITBase {

  private final DistributionService service;

  private final Integer USAGE_ID = 100000007;

  @Autowired
  public DistributionServiceChecklistbankMyBatisIT(
      DataSource dataSource, DistributionService distributionService) {
    super(dataSource);
    this.service = distributionService;
  }

  @Test
  public void testListByChecklistUsage() {
    List<Distribution> distributions = service.listByUsage(USAGE_ID, null).getResults();
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
    assertEquals(DistributionStatus.PRESENT, distribution.getStatus());
    assertEquals(ThreatStatus.NEAR_THREATENED, distribution.getThreatStatus());
    assertEquals("Hecht-Markou, 1995", distribution.getSource());
    assertNull(distribution.getAppendixCites());
    assertNull(distribution.getStartDayOfYear());
    assertNull(distribution.getLifeStage());
    assertNull(distribution.getTemporal());

    for (Distribution d : distributions) {
      assertNull(d.getSourceTaxonKey());
      assertNotNull(d.getCountry());
      assertEquals(DistributionStatus.PRESENT, d.getStatus());
    }

    assertEquals(Country.BRAZIL, distributions.get(0).getCountry()); // #15
    assertEquals(Country.FRANCE, distributions.get(1).getCountry()); // #16
    assertEquals(Country.PERU, distributions.get(2).getCountry()); // #17
    assertEquals(Country.SURINAME, distributions.get(3).getCountry()); // #18

    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    Distribution d1 = service.listByUsage(USAGE_ID, page).getResults().get(0);

    page = new PagingRequest(1, 1);
    Distribution d2 = service.listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(d1, distributions.get(0));
    assertEquals(d2, distributions.get(1));
  }

  @Test
  public void testListByRange() {
    Map<Integer, List<Distribution>> map =
        ((DistributionServiceMyBatis) service).listRange(1, 100000020);
    assertEquals(1, map.size());

    List<Distribution> records = map.get(100000007);
    assertEquals(4, records.size());
    for (Distribution v : records) {
      assertNull(v.getSourceTaxonKey());
    }
  }
}
