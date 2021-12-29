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
import org.gbif.checklistbank.service.CitationService;
import org.gbif.utils.text.StringUtils;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CitationServiceChecklistbankMyBatisIT extends ChecklistbankMyBatisServiceITBase {

  private final CitationService service;

  @Autowired
  public CitationServiceChecklistbankMyBatisIT(DataSource dataSource, CitationService citationService) {
    super(dataSource);
    this.service = citationService;
  }

  @Test
  public void testLargeCitations() throws Exception {
    String citation = StringUtils.randomString(100000);
    final Integer cid = service.createOrGet(citation);
    assertNotNull(cid);

    final Integer cid2 = service.createOrGet(citation);
    assertEquals(cid2, cid);
  }
}
