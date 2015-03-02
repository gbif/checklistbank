package org.gbif.checklistbank.ws.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SimpleParameterMapTest {

  //See http://dev.gbif.org/issues/browse/POR-2688
  @Test
  public void testParam() throws Exception {
    SimpleParameterMap params = new SimpleParameterMap();
    params.param("name", "southerni %7B%7Bnowrap Abdel-Moez & Humphries, 1955");
    assertEquals("southerni %7B%7Bnowrap Abdel-Moez & Humphries, 1955", params.get("name").get(0));

    params = new SimpleParameterMap();
    params.param("name", "southerni {{nowrap Abdel-Moez & Humphries, 1955");
    assertEquals("southerni %7B%7Bnowrap Abdel-Moez & Humphries, 1955", params.get("name").get(0));
  }
}