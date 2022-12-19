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

import org.gbif.ChecklistbankMyBatisServiceITBase;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbLoadTestDb;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.TestData;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@TestData(TestData.DATAFILE.SQUIRRELS)
public class ClbBatchServiceChecklistbankMyBatisIT extends ChecklistbankMyBatisServiceITBase {

  private final UsageService service;

  @Autowired
  public ClbBatchServiceChecklistbankMyBatisIT(UsageService usageService) {
    super();
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
      assertEquals(ClbLoadTestDb.SQUIRRELS_DATASET_KEY, nu.getDatasetKey());

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
