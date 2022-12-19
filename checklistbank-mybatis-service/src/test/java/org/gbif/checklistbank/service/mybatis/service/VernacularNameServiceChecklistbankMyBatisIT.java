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
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.api.vocabulary.Language;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.TestData;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestData(TestData.DATAFILE.SQUIRRELS)
public class VernacularNameServiceChecklistbankMyBatisIT extends ChecklistbankMyBatisServiceITBase {

  private final VernacularNameService service;

  @Autowired
  public VernacularNameServiceChecklistbankMyBatisIT(VernacularNameService vernacularNameService) {
    super();
    this.service = vernacularNameService;
  }

  @Test
  public void testListByChecklistUsage() {
    // TEST VERNACULAR
    List<VernacularName> squirrels = service.listByUsage(100000025, null).getResults();
    assertEquals(2, squirrels.size());
    assertEquals("Eurasian Red Squirrel", squirrels.get(0).getVernacularName());
    assertEquals(Language.ENGLISH, squirrels.get(0).getLanguage());
    assertEquals("Europäisches Eichhörnchen", squirrels.get(1).getVernacularName());
    assertEquals(Language.GERMAN, squirrels.get(1).getLanguage());
    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    VernacularName v1 = service.listByUsage(100000025, page).getResults().get(0);

    page = new PagingRequest(1, 1);
    VernacularName v2 = service.listByUsage(100000025, page).getResults().get(0);
    assertEquals(v1, squirrels.get(0));
    assertEquals(v2, squirrels.get(1));
  }

  @Test
  public void testListByRange() {
    // TEST VERNACULAR
    Map<Integer, List<VernacularName>> records =
        ((VernacularNameServiceMyBatis) service).listRange(1, 100000025);
    assertEquals(3, records.size());

    List<VernacularName> vernacularNames = records.get(100000025);
    assertEquals(2, vernacularNames.size());

    for (VernacularName v : vernacularNames) {
      assertNull(v.getSourceTaxonKey());
      assertNotNull(v.getVernacularName());
    }
  }
}
