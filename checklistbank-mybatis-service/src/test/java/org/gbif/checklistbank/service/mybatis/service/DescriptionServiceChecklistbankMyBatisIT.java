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
import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.TableOfContents;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.vocabulary.Language;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.TestData;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@TestData(TestData.DATAFILE.SQUIRRELS)
public class DescriptionServiceChecklistbankMyBatisIT extends ChecklistbankMyBatisServiceITBase {

  private final DescriptionService service;

  private final Integer USAGE_ID = 100000040;

  @Autowired
  public DescriptionServiceChecklistbankMyBatisIT(DescriptionService descriptionService) {
    super();
    this.service = descriptionService;
  }

  @Test
  public void testGet() {
    Description description = service.get(26);
    assertEquals((Integer) 100000040, description.getSourceTaxonKey());
    assertEquals(
        "The Caucasian squirrel (or Persian squirrel) is a tree squirrel in the genus Sciurus endemic to Armenia, Azerbaijan, Georgia, Greece, Iran, Iraq, Israel, Jordan, Lebanon, Syria, and Turkey. Its natural habitat is temperate broadleaf and mixed forests.[1]",
        description.getDescription());
    assertEquals(Language.ENGLISH, description.getLanguage());
    assertEquals("general", description.getType());
    assertNull(description.getSource());
    assertNull(description.getContributor());
    assertNull(description.getCreator());
  }

  @Test
  public void testToc() {
    TableOfContents toc = service.getToc(100000004);
    assertEquals(1, toc.listLanguages().size());
    assertEquals(4, toc.listTopicEntries(Language.ENGLISH).size());
    assertEquals(0, toc.listTopicEntries(Language.SPANISH).size());

    for (String topic : toc.listTopicEntries(Language.ENGLISH).keySet()) {
      assertFalse(toc.listTopicEntries(Language.ENGLISH).get(topic).isEmpty());
      assertNotNull(toc.listTopicEntries(Language.ENGLISH).get(topic).get(0));
      assertTrue(toc.listTopicEntries(Language.ENGLISH).get(topic).get(0) > 0);
    }

    // same via nub
    toc = service.getToc(10);
    assertEquals(1, toc.listLanguages().size());
    assertEquals(4, toc.listTopicEntries(Language.ENGLISH).size());
    assertEquals(0, toc.listTopicEntries(Language.SPANISH).size());

    for (String topic : toc.listTopicEntries(Language.ENGLISH).keySet()) {
      assertFalse(toc.listTopicEntries(Language.ENGLISH).get(topic).isEmpty());
      assertNotNull(toc.listTopicEntries(Language.ENGLISH).get(topic).get(0));
      assertTrue(toc.listTopicEntries(Language.ENGLISH).get(topic).get(0) > 0);
    }
  }

  @Test
  public void testListByChecklistUsage() {
    List<Description> descriptions = service.listByUsage(USAGE_ID, null).getResults();
    assertEquals(3, descriptions.size());
    assertEquals((Integer) 25, descriptions.get(0).getKey());
    assertEquals((Integer) 26, descriptions.get(1).getKey());
    assertEquals((Integer) 27, descriptions.get(2).getKey());

    // TEST PAGING
    Pageable page = new PagingRequest(0, 1);
    Description d1 = service.listByUsage(USAGE_ID, page).getResults().get(0);

    page = new PagingRequest(1, 1);
    Description d2 = service.listByUsage(USAGE_ID, page).getResults().get(0);
    assertEquals(d1, descriptions.get(0));
    assertEquals(d2, descriptions.get(1));
  }

  @Test
  public void testListByRange() {
    Map<Integer, List<Description>> map =
        ((DescriptionServiceMyBatis) service).listRange(1, 100000020);
    assertEquals(3, map.size());

    assertEquals(4, map.get(10).size());
    assertEquals(1, map.get(100000007).size());
    assertEquals(4, map.get(100000004).size());

    for (Description v : map.get(100000004)) {
      // checklist records
      assertNull(v.getSourceTaxonKey());
      assertNotNull(v.getDescription());
      assertNotNull(v.getKey());
    }
    for (Description v : map.get(10)) {
      // nub records
      assertNotNull(v.getSourceTaxonKey());
      assertNotNull(v.getDescription());
      assertNotNull(v.getKey());
    }
  }
}
