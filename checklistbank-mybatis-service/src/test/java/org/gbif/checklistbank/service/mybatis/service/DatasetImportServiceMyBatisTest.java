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

import org.gbif.api.model.checklistbank.Reference;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DatasetImportServiceMyBatisTest {

  @Test
  public void testBuildCitation() throws Exception {
    Reference r = new Reference();
    assertNull(UsageSyncServiceMyBatis.buildCitation(r));

    r.setTitle("Fruitflies of Europe");
    assertEquals("Fruitflies of Europe", UsageSyncServiceMyBatis.buildCitation(r));

    r.setAuthor("Simmons, R.C");
    assertEquals("Simmons, R.C: Fruitflies of Europe", UsageSyncServiceMyBatis.buildCitation(r));

    r.setDate("1982");
    assertEquals(
        "Simmons, R.C (1982) Fruitflies of Europe", UsageSyncServiceMyBatis.buildCitation(r));

    r.setSource("Animalia minor Vol.34");
    assertEquals(
        "Simmons, R.C (1982) Fruitflies of Europe: Animalia minor Vol.34",
        UsageSyncServiceMyBatis.buildCitation(r));
  }
}
