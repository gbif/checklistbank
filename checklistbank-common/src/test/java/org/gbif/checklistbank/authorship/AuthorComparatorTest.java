package org.gbif.checklistbank.authorship;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.model.Equality;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AuthorComparatorTest {
  AuthorComparator comp = AuthorComparator.createWithAuthormap();

  @Test
  public void testNormalize() throws Exception {
    assertNull(comp.normalize(null));
    assertNull(comp.normalize(" "));
    assertNull(comp.normalize("."));
    assertNull(comp.normalize(" (-) "));

    assertEquals("doring", comp.normalize("Döring"));
    assertEquals("desireno", comp.normalize("Désírèñø"));

    assertEquals("a j white", comp.normalize("A.J. White"));
    assertEquals("j a white", comp.normalize("J A  WHITE"));
    assertEquals("a j white", comp.normalize("A-J-White"));
    assertEquals("a j white", comp.normalize("(A.J. White)"));

    assertEquals("white herbert harvey", comp.normalize("A. J. White, Herbert  &  P. J. Harvey"));
    assertEquals("white herbert harvey", comp.normalize("A.J. White, Herbert et P.J. Harvey"));
    assertEquals("white herbert harvey", comp.normalize("A.J. White, Herbert and P.J. Harvey"));

    assertEquals("colla", comp.normalize("Bertero ex Colla"));
    assertEquals("schult", comp.normalize("Sieber ex Schult."));
    assertEquals("nevski", comp.normalize("Desv. ex Nevski"));
    assertEquals("w q yin", comp.normalize("K. M. Feng ex W. Q. Yin"));

    assertEquals("g kirchn", comp.normalize("G.Kirchn. in Petzold & G.Kirchn."));

    assertEquals("torr gray", comp.normalize("Torr. & A.Gray"));
    assertEquals("c chr", comp.normalize("C. Chr."));
    assertEquals("h christ", comp.normalize("H. Christ"));
  }

  @Test
  public void testEquals() throws Exception {
    ParsedName p1 = new ParsedName();
    ParsedName p2 = new ParsedName();

    assertEquals(Equality.UNKNOWN, comp.compare(p1, p2));

    p1.setAuthorship("L.");
    assertEquals(Equality.UNKNOWN, comp.compare(p1, p2));

    p2.setAuthorship("Linne");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p1.setAuthorship("Linné");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p1.setYear("1847");
    p2.setYear("1877");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p2.setAuthorship("Carl von Linne");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p2.setYear("1847");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));
  }

  @Test
  public void testEquals2() throws Exception {
    assertAuth(null, null, Equality.UNKNOWN, null, null);
    assertAuth("", "  ", Equality.UNKNOWN, " ", "   ");
    assertAuth("L.", null, Equality.UNKNOWN, null, null);

    assertAuth("Bluff & Fingerh.", null, Equality.DIFFERENT, "Lindl.", null);
    assertAuth("Lindl.", null, Equality.EQUAL, "Lindl.", null);

    assertAuth(null, "1978", Equality.DIFFERENT, null, "1934");
    assertAuth(null, "1978", Equality.EQUAL, null, "1978");

    assertAuth("H. Christ", null, Equality.DIFFERENT, "C. Chr.", null);
  }

  @Test
  public void testDiffer() throws Exception {
    ParsedName p1 = new ParsedName();
    p1.setAuthorship("Reich.");

    ParsedName p2 = new ParsedName();
    p2.setAuthorship("");
    assertEquals(Equality.UNKNOWN, comp.compare(p1, p2));

    p2.setAuthorship("Müller");
    assertEquals(Equality.DIFFERENT, comp.compare(p1, p2));

    p2.setAuthorship("Jenkins, Marx & Kluse");
    assertEquals(Equality.DIFFERENT, comp.compare(p1, p2));
  }

  @Test
  public void testEqualsWithBasionym() throws Exception {
    assertAuth("A.Murr", "1863", null, null, Equality.EQUAL, "A.Murray bis.", null, null, null);
    assertAuth("Maxim.", null, Equality.EQUAL, "Max.", null);
    assertAuth("Maxim.", null, "Trautv. ex Maxim.", null, Equality.EQUAL, "Maxim.", null, null, null);
    assertAuth("Maxim.", null, "Trautv. ex Karl Johann Maximowicz", null, Equality.EQUAL, "Max.", null, null, null);
    assertAuth("Maxim.", null, null, null, Equality.EQUAL, "Karl Johann Maximowicz", null, null, null);

    assertAuth("Bluff & Fingerh.", null, "L.", null, Equality.DIFFERENT, "Mill.", "1768", null, null);

    assertAuth("A.Murr", "1863", null, null, Equality.EQUAL, "A. Murray", null, null, null);
    assertAuth("A.Murr", "1863", null, null, Equality.EQUAL, "A.Murray bis.", null, null, null);
    assertAuth("A.Murr", "1863", null, null, Equality.EQUAL, "A. Murr.", null, null, null);
    assertAuth("A.Murr", "1863", null, null, Equality.DIFFERENT, "B. Murr.", null, null, null);

    assertAuth("Debreczy & I. Rácz", null, null, null, Equality.EQUAL, "Debreczy & Rácz", null, null, null);
    assertAuth("Debreczy & I.Rácz", null, null, null, Equality.DIFFERENT, "Silba", "Debreczy & I.Rácz", null, null);
  }

  private void assertAuth(String a1, String y1, Equality eq, String a2, String y2) {
    assertEquals(eq, comp.compare(a1, y1, a2, y2));
  }

  private void assertAuth(String a1, String y1, String a1b, String y1b, Equality eq, String a2, String y2, String a2b, String y2b) {
    ParsedName p1 = new ParsedName();
    p1.setAuthorsParsed(true);
    p1.setAuthorship(a1);
    p1.setYear(y1);
    p1.setBracketAuthorship(a1b);
    p1.setBracketYear(y1b);

    ParsedName p2 = new ParsedName();
    p2.setAuthorsParsed(true);
    p2.setAuthorship(a2);
    p2.setYear(y2);
    p2.setBracketAuthorship(a2b);
    p2.setBracketYear(y2b);

    assertEquals(eq, comp.compare(p1, p2));
  }

  @Test
  public void testEqualsSubstring() throws Exception {
    ParsedName p1 = new ParsedName();
    ParsedName p2 = new ParsedName();

    p1.setAuthorship("L.");
    p2.setAuthorship("Linne");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p2.setAuthorship("Lin.");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p1.setAuthorship("DC.");
    p2.setAuthorship("De Candolle");
    assertEquals(Equality.DIFFERENT, comp.compare(p1, p2));

    p2.setAuthorship("DCandolle");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p1.setAuthorship("Miller");
    p2.setAuthorship("Mill.");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p1.setAuthorship("Hern.");
    p2.setAuthorship("Hernandez");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p1.setAuthorship("Robertson, T., Miller, P. et Jameson, R. J.");
    p2.setAuthorship("Miller");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p1.setAuthorship("T. Robertson, P. Miller & R.J. Jameson");
    p2.setAuthorship("Miller");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p2.setAuthorship("Mülles");
    assertEquals(Equality.DIFFERENT, comp.compare(p1, p2));
  }


  @Test
  public void testBlattariaAuthors() throws Exception {
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
        assertEquals(Equality.DIFFERENT, comp.compare(p1, p));
      }
      if (!p2.equals(p)) {
        assertEquals(Equality.DIFFERENT, comp.compare(p2, p));
      }
      if (!p3.equals(p)) {
        assertEquals(Equality.DIFFERENT, comp.compare(p3, p));
      }
      if (!p4.equals(p)) {
        assertEquals(Equality.DIFFERENT, comp.compare(p4, p));
      }
    }
  }

  @Test
  public void testUnparsedAuthors() throws Exception {
    ParsedName p3 = new ParsedName();
    p3.setAuthorsParsed(false);
    p3.setScientificName("Blattaria Voet, ?, 1806");
    p3.setGenusOrAbove("Blattaria");

    ParsedName p4 = new ParsedName();
    p4.setAuthorsParsed(true);
    p4.setScientificName("Blattaria Weyenbergh, 1874");
    p4.setAuthorship("Weyenbergh");
    p4.setYear("1874");

    assertEquals(Equality.DIFFERENT, comp.compare(p3, p4));

    p4.setYear("1806");
    assertEquals(Equality.EQUAL, comp.compare(p3, p4));
  }

  @Test
  public void testAlikeAuthors() throws Exception {
    ParsedName p1 = new ParsedName();
    p1.setAuthorsParsed(true);
    p1.setAuthorship("Voet");

    ParsedName p2 = new ParsedName();
    p2.setAuthorsParsed(true);
    p2.setAuthorship("Weyenbergh");

    assertEquals(Equality.DIFFERENT, comp.compare(p1, p2));

    p2 = new ParsedName();
    p2.setAuthorship("Voetington");

    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p2 = new ParsedName();
    p2.setAuthorship("Vellington");

    assertEquals(Equality.DIFFERENT, comp.compare(p1, p2));
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-398
   */
  @Test
  public void testEqualsProper() throws Exception {
    assertFalse(comp.equals(null, null, null, null));
    assertFalse(comp.equals("", "", "", ""));
    assertFalse(comp.equals("", null, "", null));

    assertTrue(comp.equals("Ridgway", "1893", "Ridgway", "1893"));
    assertTrue(comp.equals("K. Koch", null, "K. Koch", null));
    assertTrue(comp.equals("K.Koch", null, "K. Koch", null));
    assertTrue(comp.equals("A. Nelson", null, "A Nélson", null));
    assertTrue(comp.equals("Colla", null, "Bertero ex Colla", null));

    assertFalse(comp.equals("Oberholser", "1917", "Oberholser", "1919"));
    assertFalse(comp.equals("Koch", "1897", "K. Koch", null));
    assertFalse(comp.equals("Taczanowski & Berlepsch", "1885", "Berlepsch & Taczanowski", "1884"));
    assertFalse(comp.equals("Gould", "1860", "Gould", "1861"));
    assertFalse(comp.equals("Gould", "1860", "Gould", "1863"));
    assertFalse(comp.equals("A. Nelson", null, "E.E. Nelson", null));

    //TODO: improve author comparator to deal with these cases which should be a positiv match:
    //assertTrue(comp.equals("Koch", null, "K. Koch", null));
    //assertTrue(comp.equals("Taczanowski & Berlepsch", "1885", "Berlepsch & Taczanowski", "1885"));
  }
}