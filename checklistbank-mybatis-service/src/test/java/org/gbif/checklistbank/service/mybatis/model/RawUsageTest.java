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
package org.gbif.checklistbank.service.mybatis.model;

import org.gbif.checklistbank.model.RawUsage;

import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class RawUsageTest {

  @Test
  public void testEquals() throws Exception {
    final int key = 100000001;
    final String json = "xeghwax542tgld@";
    final UUID dkey = UUID.randomUUID();
    final Date now = new Date();

    RawUsage raw = new RawUsage();
    raw.setUsageKey(key);
    raw.setDatasetKey(dkey);
    raw.setLastCrawled(now);
    raw.setJson(json);

    RawUsage raw2 = new RawUsage();
    raw2.setUsageKey(key);
    raw2.setDatasetKey(dkey);
    raw2.setLastCrawled(now);
    raw2.setJson(json);

    assertEquals(raw, raw2);

    raw.setLastCrawled(null);
    assertNotEquals(raw, raw2);

    raw2.setLastCrawled(null);
    assertEquals(raw, raw2);
  }
}
