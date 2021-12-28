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
package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Maps;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HstoreCountTypeHandlerTest {

  @Test
  public void testSortMap() throws Exception {
    HashMap<String, Integer> map = Maps.newHashMap();
    map.put("Mimi", 10);
    map.put("Harry", 100);
    map.put("Alberto", 2);
    map.put("Tim", 2);
    map.put("Svenja", -18);
    map.put("Fritz", 10974);

    Map<String, Integer> sorted = HstoreCountTypeHandler.sortMapByValuesDesc(map);
    Integer last = null;
    for (Map.Entry<String, Integer> e : sorted.entrySet()) {
      if (last != null) {
        assertTrue(last >= e.getValue());
      }
      last = e.getValue();
    }
  }
}
