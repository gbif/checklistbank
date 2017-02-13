package org.gbif.checklistbank.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class SymmetricIdentityMatrixTest {

  @Test
  public void add() throws Exception {
    SymmetricIdentityMatrix<Integer> m = SymmetricIdentityMatrix.create();

    assertTrue(m.contains(1,1));
    assertFalse(m.contains(1,2));

    m.add(1,2);
    assertTrue(m.contains(1,1));
    assertTrue(m.contains(1,2));
    assertTrue(m.contains(2,1));
    assertFalse(m.contains(1,3));

    m.add(2,1);
    assertTrue(m.contains(1,1));
    assertTrue(m.contains(1,2));
    assertTrue(m.contains(2,1));
    assertFalse(m.contains(1,3));

    m.add(3,1);
    assertTrue(m.contains(1,1));
    assertTrue(m.contains(1,2));
    assertTrue(m.contains(2,1));
    assertTrue(m.contains(1,3));
    assertTrue(m.contains(3, 1));

    m.remove(2);
    assertTrue(m.contains(1,1));
    assertFalse(m.contains(1,2));
    assertFalse(m.contains(2,1));
    assertTrue(m.contains(2,2));
    assertTrue(m.contains(1,3));
    assertTrue(m.contains(3, 1));
  }

}