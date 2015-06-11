package org.gbif.checklistbank.nub.authorship;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.nub.model.Equality;

import java.util.List;

import com.google.common.collect.Lists;
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
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p1.setAuthorship("Linné");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p1.setYear("1847");
    p2.setYear("1877");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p2.setAuthorship("Carl von Linne");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p2.setYear("1847");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));
  }

  @Test
  public void testEqualsSubstring() throws Exception {
    AuthorComparator comp = new AuthorComparator();

    ParsedName p1 = new ParsedName();
    ParsedName p2 = new ParsedName();

    p1.setAuthorship("L.");
    p2.setAuthorship("Linne");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p2.setAuthorship("Lin.");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p1.setAuthorship("DC.");
    p2.setAuthorship("De Candolle");
    assertEquals(Equality.DIFFERENT, comp.equals(p1, p2));

    p2.setAuthorship("DCandolle");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p1.setAuthorship("Miller");
    p2.setAuthorship("Mill.");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p1.setAuthorship("Hern.");
    p2.setAuthorship("Hernandez");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p1.setAuthorship("Robertson, T., Miller, P. et Jameson, R. J.");
    p2.setAuthorship("Miller");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p1.setAuthorship("T. Robertson, P. Miller & R.J. Jameson");
    p2.setAuthorship("Miller");
    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p2.setAuthorship("Mülles");
    assertEquals(Equality.DIFFERENT, comp.equals(p1, p2));
  }


  @Test
  public void testBlattariaAuthors() throws Exception {
    AuthorComparator comp = new AuthorComparator();

    ParsedName p1 = new ParsedName();
    p1.setAuthorship("P. Miller");
    p1.setYear("1754");

    ParsedName p2 = new ParsedName();
    p2.setAuthorship("O. Kuntze");
    p2.setYear("1891");

    ParsedName p3 = new ParsedName();
    p3.setAuthorship("Voet, ?");
    p3.setYear("1806");

    ParsedName p4 = new ParsedName();
    p4.setAuthorship("Weyenbergh");
    p4.setYear("1874");

    List<ParsedName> names = Lists.newArrayList(p1, p2, p3, p4);
    for (ParsedName p : names) {
      if (!p1.equals(p)) {
        assertEquals(Equality.DIFFERENT, comp.equals(p1, p));
      }
      if (!p2.equals(p)) {
        assertEquals(Equality.DIFFERENT, comp.equals(p2, p));
      }
      if (!p3.equals(p)) {
        assertEquals(Equality.DIFFERENT, comp.equals(p3, p));
      }
      if (!p4.equals(p)) {
        assertEquals(Equality.DIFFERENT, comp.equals(p4, p));
      }
    }
  }

  @Test
  public void testUnparsedAuthors() throws Exception {
    AuthorComparator comp = new AuthorComparator();

    ParsedName p3 = new ParsedName();
    p3.setAuthorsParsed(false);
    p3.setScientificName("Blattaria Voet, ?, 1806");
    p3.setGenusOrAbove("Blattaria");

    ParsedName p4 = new ParsedName();
    p4.setAuthorsParsed(true);
    p4.setScientificName("Blattaria Weyenbergh, 1874");
    p4.setAuthorship("Weyenbergh");
    p4.setYear("1874");

    assertEquals(Equality.DIFFERENT, comp.equals(p3, p4));

    p4.setYear("1806");
    assertEquals(Equality.EQUAL, comp.equals(p3, p4));
 }

  @Test
  public void testAlikeAuthors() throws Exception {
    AuthorComparator comp = new AuthorComparator();

    ParsedName p1 = new ParsedName();
    p1.setAuthorsParsed(true);
    p1.setAuthorship("Voet");

    ParsedName p2 = new ParsedName();
    p2.setAuthorsParsed(true);
    p2.setAuthorship("Weyenbergh");

    assertEquals(Equality.DIFFERENT, comp.equals(p1, p2));

    p2 = new ParsedName();
    p2.setAuthorship("Voetington");

    assertEquals(Equality.EQUAL, comp.equals(p1, p2));

    p2 = new ParsedName();
    p2.setAuthorship("Vellington");

    assertEquals(Equality.DIFFERENT, comp.equals(p1, p2));
  }
}