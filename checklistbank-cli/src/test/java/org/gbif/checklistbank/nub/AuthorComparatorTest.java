package org.gbif.checklistbank.nub;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.nub.model.Equality;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AuthorComparatorTest {

  @Test
  public void testNormalize() throws Exception {
    assertNull(AuthorComparator.normalize(null));
    assertNull(AuthorComparator.normalize(" "));
    assertNull(AuthorComparator.normalize("."));
    assertNull(AuthorComparator.normalize(" (-) "));

    assertEquals("doring", AuthorComparator.normalize("Döring"));
    assertEquals("desireno", AuthorComparator.normalize("Désírèñø"));

    assertEquals("a j white", AuthorComparator.normalize("A.J. White"));
    assertEquals("j a white", AuthorComparator.normalize("J A  WHITE"));
    assertEquals("a j white", AuthorComparator.normalize("A-J-White"));
    assertEquals("a j white", AuthorComparator.normalize("(A.J. White)"));

    assertEquals("a j white herbert p j harvey", AuthorComparator.normalize("A. J. White, Herbert  &  P. J. Harvey"));
    assertEquals("a j white herbert p j harvey", AuthorComparator.normalize("A.J. White, Herbert et P.J. Harvey"));
    assertEquals("a j white herbert p j harvey", AuthorComparator.normalize("A.J. White, Herbert and P.J. Harvey"));
  }

  @Test
  public void testEquals() throws Exception {
    AuthorComparator comp = new AuthorComparator();

    ParsedName p1 = new ParsedName();
    ParsedName p2 = new ParsedName();

    assertEquals(Equality.UNKNOWN, comp.equals(p1, p2));

    p1.setAuthorship("L.");
    assertEquals(Equality.UNKNOWN, comp.equals(p1, p2));

    p2.setAuthorship("Linne");
    assertEquals(Equality.DIFFERENT, comp.equals(p1, p2));

    p1.setAuthorship("Linné");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p1.setYear("1847");
    p2.setYear("1877");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p2.setAuthorship("Carl von Linne");
    assertEquals(Equality.DIFFERENT, comp.equals(p1, p2));

    p2.setYear("1847");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));
  }
}