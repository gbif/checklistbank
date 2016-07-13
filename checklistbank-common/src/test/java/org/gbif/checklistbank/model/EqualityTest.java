package org.gbif.checklistbank.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class EqualityTest {

  @Test
  public void testAnd() throws Exception {
    assertEquals(Equality.EQUAL, Equality.EQUAL.and(Equality.EQUAL));
    assertEquals(Equality.EQUAL, Equality.EQUAL.and(Equality.UNKNOWN));
    assertEquals(Equality.EQUAL, Equality.UNKNOWN.and(Equality.EQUAL));

    assertEquals(Equality.UNKNOWN, Equality.UNKNOWN.and(Equality.UNKNOWN));

    assertEquals(Equality.DIFFERENT, Equality.DIFFERENT.and(Equality.DIFFERENT));
    assertEquals(Equality.DIFFERENT, Equality.UNKNOWN.and(Equality.DIFFERENT));
    assertEquals(Equality.DIFFERENT, Equality.DIFFERENT.and(Equality.UNKNOWN));
    assertEquals(Equality.DIFFERENT, Equality.EQUAL.and(Equality.DIFFERENT));
    assertEquals(Equality.DIFFERENT, Equality.DIFFERENT.and(Equality.EQUAL));
  }
}