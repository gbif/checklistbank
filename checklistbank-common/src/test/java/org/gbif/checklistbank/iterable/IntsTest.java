package org.gbif.checklistbank.iterable;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class IntsTest {

  @Test
  public void testRange() throws Exception {
    int counter = 4;
    for (int i : Ints.range(4,8)) {
      assertEquals(counter++, i);
    }
    assertEquals(9, counter);

    counter = -12;
    for (int i : Ints.range(-12,0)) {
      assertEquals(counter++, i);
    }
    assertEquals(1, counter);

    boolean did12=false;
    for (int i : Ints.range(12,12)) {
      assertEquals(12, i);
      did12=true;
    }
    assertTrue(did12);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidRange() throws Exception {
    Ints.range(12,8);
  }

  @Test
  public void testUntil() throws Exception {
    int counter = 1;
    for (int i : Ints.until(8)) {
      assertEquals(counter++, i);
    }
    assertEquals(9, counter);
  }
}