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
import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.ReferenceService;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.TestData;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestData(TestData.DATAFILE.SQUIRRELS)
public class ReferenceServiceChecklistbankMyBatisIT extends ChecklistbankMyBatisServiceITBase {

  private final ReferenceService service;

  private final Integer USAGE_ID = 100000025;

  @Autowired
  public ReferenceServiceChecklistbankMyBatisIT(DataSource dataSource, ReferenceService referenceService) {
    super(dataSource);
    this.service = referenceService;
  }

  private void verify17(Reference ref) {
    assertEquals(
        "Watson, Jeremy (30 December 2007) \"Tufty's saviour to the rescue\". Scotland on Sunday. Edinburgh.",
        ref.getCitation());
    assertNull(ref.getAuthor());
    assertNull(ref.getTitle());
    assertNull(ref.getDate());
    assertNull(ref.getType());
    assertNull(ref.getLink());
    assertNull(ref.getDoi());
    assertNull(ref.getRemarks());
    assertNull(ref.getSourceTaxonKey());
  }

  @Test
  public void testListByChecklistUsage() {
    List<Reference> refs = service.listByUsage(USAGE_ID, null).getResults();
    assertEquals(20, refs.size());
    assertEquals(
        "\"A new era for Scotland's red squirrels?\" in Scottish Wildlife (November 2008) No. 66. Edinburgh.",
        refs.get(0).getCitation());
    assertEquals(
        "\"Black squirrels set to dominate\". BBC News. 2009-01-20. Retrieved 2009-04-26.",
        refs.get(1).getCitation());

    verify17(refs.get(16));

    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    Reference r1 = service.listByUsage(USAGE_ID, page).getResults().get(0);

    page = new PagingRequest(1, 1);
    Reference r2 = service.listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(r1, refs.get(0));
    assertEquals(r2, refs.get(1));
  }
}
