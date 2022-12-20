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
import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.IdentifierService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.TestData;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestData(TestData.DATAFILE.SQUIRRELS)
public class IdentifierServiceChecklistbankMyBatisIT extends ChecklistbankMyBatisServiceITBase {

  private final IdentifierService service;

  private final Integer USAGE_ID = 100000007;

  @Autowired
  public IdentifierServiceChecklistbankMyBatisIT(IdentifierService identifierService) {
    super();
    this.service = identifierService;
  }

  @Test
  public void testListByUsage() {
    List<Identifier> ids = service.listByUsage(USAGE_ID, null).getResults();
    assertEquals(3, ids.size());

    Set<IdentifierType> types =
        Sets.newHashSet(IdentifierType.DOI, IdentifierType.URL, IdentifierType.LSID);
    for (Identifier id : ids) {
      switch (id.getType()) {
        case LSID:
          assertEquals(
              "urn:lsid:catalogueoflife.org:taxon:df0a319c-29c1-102b-9a4a-00304854f820:col20120721",
              id.getIdentifier());
          assertNull(id.getTitle());
          break;
        case DOI:
          assertEquals("doi:10.1038/6905528", id.getIdentifier());
          assertNull(id.getTitle());
          break;
        case URL:
          assertEquals(
              "http://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=632417",
              id.getIdentifier());
          assertNull(id.getTitle());
          break;
        default:
          fail("Unkown Identifier");
      }
      types.remove(id.getType());
    }
    assertTrue(types.isEmpty());
    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    Identifier d1 = service.listByUsage(USAGE_ID, page).getResults().get(0);

    page = new PagingRequest(1, 1);
    Identifier d2 = service.listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(d1, ids.get(0));
    assertEquals(d2, ids.get(1));
  }
}
