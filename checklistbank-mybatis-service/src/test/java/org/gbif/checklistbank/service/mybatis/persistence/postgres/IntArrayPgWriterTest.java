package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IntArrayPgWriterTest {

  @Test
  public void testWrite() throws Exception {
    assertInput("12\n54321\n23456789\n345670981");
    assertInput("12\n54321\n23456789\n345670981\n");
  }

  private void assertInput(final String input) throws Exception {
    IntArrayPgWriter writer = new IntArrayPgWriter();
    writer.write(input.toCharArray());
    List<Integer> result = writer.result();
    assertEquals(4, result.size());
    assertTrue(result.contains(12));
    assertTrue(result.contains(54321));
    assertTrue(result.contains(23456789));
    assertTrue(result.contains(345670981));
  }
}
