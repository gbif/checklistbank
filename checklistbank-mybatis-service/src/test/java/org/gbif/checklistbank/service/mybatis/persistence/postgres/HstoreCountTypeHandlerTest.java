package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

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
