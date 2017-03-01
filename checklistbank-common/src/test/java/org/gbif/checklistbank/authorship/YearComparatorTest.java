package org.gbif.checklistbank.authorship;

import org.gbif.checklistbank.model.Equality;
import org.junit.Test;

import static org.junit.Assert.*;

public class YearComparatorTest {

  @Test
  public void compare() throws Exception {
    assertEquals(Equality.UNKNOWN, new YearComparator("123", null).compare());
    assertEquals(Equality.UNKNOWN, new YearComparator("123", "").compare());
    assertEquals(Equality.UNKNOWN, new YearComparator("123", " ").compare());
    assertEquals(Equality.UNKNOWN, new YearComparator(null, null).compare());

    assertEquals(Equality.DIFFERENT, new YearComparator("neunzehn", "1887").compare());
    assertEquals(Equality.DIFFERENT, new YearComparator("neunzehn", "3 plus").compare());
    assertEquals(Equality.DIFFERENT, new YearComparator("123", "125").compare());
    assertEquals(Equality.DIFFERENT, new YearComparator("2001", "1999").compare());

    assertEquals(Equality.EQUAL, new YearComparator(" 1678", "1678 ").compare());

    // 1 year diff is ok
    assertEquals(Equality.EQUAL, new YearComparator("2001", "2001").compare());
    assertEquals(Equality.EQUAL, new YearComparator("2001", "2000").compare());
    assertEquals(Equality.EQUAL, new YearComparator("2000", "1999").compare());
    assertEquals(Equality.EQUAL, new YearComparator("189", "190").compare());

    // placeholders accepted
    assertEquals(Equality.EQUAL, new YearComparator("1878", "187?").compare());
    assertEquals(Equality.EQUAL, new YearComparator("1878", "1879?").compare());
    assertEquals(Equality.EQUAL, new YearComparator("18?8", "1878").compare());
    assertEquals(Equality.EQUAL, new YearComparator("187?", "187 ?").compare());

    // but not more than one
    assertEquals(Equality.DIFFERENT, new YearComparator("18??", "1877").compare());
    assertEquals(Equality.DIFFERENT, new YearComparator("18??", "18?").compare());
  }

}