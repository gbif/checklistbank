package org.gbif.checklistbank.authorship;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NamePart;
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

    assertEquals("a j white,herbert,p j harvey", comp.normalize("A. J. White, Herbert  &  P. J. Harvey"));
    assertEquals("a j white,herbert,p j harvey", comp.normalize("A.J. White, Herbert et P.J. Harvey"));
    assertEquals("a j white,herbert,p j harvey", comp.normalize("A.J. White, Herbert and P.J. Harvey"));

    assertEquals("colla", comp.normalize("Bertero ex Colla"));
    assertEquals("schult", comp.normalize("Sieber ex Schult."));
    assertEquals("nevski", comp.normalize("Desv. ex Nevski"));
    assertEquals("w q yin", comp.normalize("K. M. Feng ex W. Q. Yin"));

    assertEquals("g kirchn", comp.normalize("G.Kirchn. in Petzold & G.Kirchn."));

    assertEquals("torr,a gray", comp.normalize("Torr. & A.Gray"));
    assertEquals("c chr", comp.normalize("C. Chr."));
    assertEquals("h christ", comp.normalize("H. Christ"));

    assertEquals("l", comp.normalize("L."));
    assertEquals("rchb", comp.normalize("Rchb."));
    assertEquals("rchb", comp.normalize("Abasicarpon Andrz. ex Rchb."));

    assertEquals("muller", comp.normalize("Müller"));
    assertEquals("muller", comp.normalize("Mueller"));
    assertEquals("moller", comp.normalize("Moeller"));

    assertEquals("don filius", comp.normalize("Don f."));
    assertEquals("don filius", comp.normalize("Don fil."));
    assertEquals("don filius", comp.normalize("Don fil"));
    assertEquals("f merck", comp.normalize("f. Merck"));
    assertEquals("f merck", comp.normalize("f Merck"));
    assertEquals("la don filius,dc", comp.normalize("la Don f. et DC"));
    assertEquals("la don,f rich,dc", comp.normalize("la Don, f. Rich. et DC"));
    assertEquals("la don,rich filius,dc", comp.normalize("la Don, Rich. f. et DC"));
    assertEquals("l filius", comp.normalize("L.f."));
    assertEquals("l filius", comp.normalize("L. f."));
    assertEquals("l filius", comp.normalize("L f"));
    assertEquals("lf", comp.normalize("Lf"));
  }

  @Test
  public void testLookup() throws Exception {
    assertNull(comp.lookup(null));
    assertEquals(" ", comp.lookup(" "));
    assertEquals(".", comp.lookup("."));
    assertEquals("-", comp.lookup("-"));

    assertEquals("Döring", comp.lookup("Döring"));
    assertEquals("desireno", comp.lookup("desireno"));

    assertEquals("a j white", comp.lookup("a j white"));

    assertEquals("l a colla", comp.lookup("colla"));
    assertEquals("j a schultes", comp.lookup("schult"));
    assertEquals("s a nevski", comp.lookup("nevski"));
    assertEquals("w q yin", comp.lookup("w q yin"));

    assertEquals("g kirchner", comp.lookup("g kirchn"));

    assertEquals("c f a christensen", comp.lookup("c chr"));
    assertEquals("h christ", comp.lookup("h christ"));

    assertEquals("c linnaus", comp.lookup("l"));
    assertEquals("h g l reichenbach", comp.lookup("rchb"));
    assertEquals("a p de candolle", comp.lookup("dc"));
    assertEquals("j lamarck", comp.lookup("lam"));
    // the input is a single author. so expect nothing
    assertEquals("lam,dc", comp.lookup("lam,dc"));

    assertEquals("c linnaus filius", comp.lookup("l filius"));
    assertEquals("c h bipontinus schultz", comp.lookup("sch bip"));
    assertEquals("c h bipontinus schultz", comp.lookup("schultz bip"));
  }

  @Test
  public void extractSurname() throws Exception {
    assertEquals("doring", comp.extractSurname("doring"));
    assertEquals("white", comp.extractSurname("a j white"));
    assertEquals("harvey", comp.extractSurname("white herbert harvey"));
    assertEquals("colla", comp.extractSurname("l a colla"));
    assertEquals("yin", comp.extractSurname("w q yin"));
    assertEquals("kirchner", comp.extractSurname("g kirchner"));
    assertEquals("reichenbach", comp.extractSurname("h g l reichenbach"));
    assertEquals("linnaeus", comp.extractSurname("c linnaeus filius"));
  }

  @Test
  public void firstInitialsDiffer() throws Exception {
    assertTrue(comp.firstInitialsDiffer("a a mark", "a b mark"));
    assertFalse(comp.firstInitialsDiffer("k f mark", "k f mark"));
    assertFalse(comp.firstInitialsDiffer("k f mark", "f k mark"));
    assertFalse(comp.firstInitialsDiffer("k f mark", "k mark"));
    assertFalse(comp.firstInitialsDiffer("k mark", "k f mark"));
    assertFalse(comp.firstInitialsDiffer("f mark", "k f mark"));
    assertFalse(comp.firstInitialsDiffer("f mark", "f k mark"));
    assertFalse(comp.firstInitialsDiffer("f mark", "f k c g s mark"));

    assertTrue(comp.firstInitialsDiffer("k mark", "f mark"));
    assertTrue(comp.firstInitialsDiffer("k f mark", "a f mark"));
    assertTrue(comp.firstInitialsDiffer("a a mark", "a b mark"));
  }

  @Test
  public void testCompareParsedName() throws Exception {
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



    p1 = new ParsedName();
    p1.setAuthorship("Reich.");

    p2 = new ParsedName();
    p2.setAuthorship("");
    assertEquals(Equality.UNKNOWN, comp.compare(p1, p2));

    p2.setAuthorship("Reichen.");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p2.setAuthorship("Reichenbrg.");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p2.setAuthorship("Reichenberger");
    assertEquals(Equality.EQUAL, comp.compare(p1, p2));

    p2.setAuthorship("Müller");
    assertEquals(Equality.DIFFERENT, comp.compare(p1, p2));

    p2.setAuthorship("Jenkins, Marx & Kluse");
    assertEquals(Equality.DIFFERENT, comp.compare(p1, p2));

    p1.setAuthorship("Mill.");
    p2.setAuthorship("L.");
    assertEquals(Equality.DIFFERENT, comp.compare(p1, p2));
  }

  @Test
  public void testCompareUnparsedAuthor() throws Exception {
    ParsedName p1 = new ParsedName();
    p1.setScientificName("Platanus x hispanica Mill. ex Münch., 1770");
    p1.setGenusOrAbove("Platanus");
    p1.setSpecificEpithet("hispanica");
    p1.setNotho(NamePart.SPECIFIC);
    p1.setAuthorsParsed(false);

    ParsedName p2 = new ParsedName();
    p2.setScientificName("Platanus hispanica Mill. ex Münch.");
    p2.setGenusOrAbove("Platanus");
    p2.setSpecificEpithet("hispanica");
    p2.setNotho(NamePart.SPECIFIC);
    p2.setAuthorship("Mill. ex Münch.");
    p2.setAuthorsParsed(true);

    assertEquals(Equality.EQUAL, comp.compare(p1, p2));
  }

  @Test
  public void testCompare() throws Exception {
    assertAuth("Debreczy & I. Rácz", null, Equality.EQUAL, "Rácz", null);
    assertAuth("DC. ex Lam. et DC.", null, Equality.EQUAL, "DC.", null);

    assertAuth(null, null, Equality.UNKNOWN, null, null);
    assertAuth("", "  ", Equality.UNKNOWN, " ", "   ");
    assertAuth("L.", null, Equality.UNKNOWN, null, null);

    assertAuth("Bluff & Fingerh.", null, Equality.DIFFERENT, "Lindl.", null);
    assertAuth("Lindl.", null, Equality.EQUAL, "Lindl.", null);

    assertAuth(null, "1978", Equality.DIFFERENT, null, "1934");
    assertAuth(null, "1978", Equality.EQUAL, null, "1978");

    assertAuth("H. Christ", null, Equality.DIFFERENT, "C. Chr.", null);
    assertAuth("Reichenbach", "1837", Equality.EQUAL, "Abasicarpon Andrz. ex Rchb.", null);

    assertAuth("Torr et Gray", null, Equality.EQUAL, "Torr. & A.Gray", null);
    assertAuth("A.Murr", "1863", Equality.EQUAL, "A.Murray bis.", null);
    assertAuth("Maxim.", null, Equality.EQUAL, "Max.", null);

    assertAuth("A.Murr", "1863", Equality.EQUAL, "A. Murray", null);
    assertAuth("A.Murr", "1863", Equality.EQUAL, "A.Murray bis.", null);
    assertAuth("A.Murr", "1863", Equality.EQUAL, "A. Murr.", null);
    assertAuth("A.Murr", "1863", Equality.DIFFERENT, "B. Murr.", null);

    assertAuth("Debreczy & I. Rácz", null, Equality.EQUAL, "Rácz", null);
    assertAuth("Debreczy & I. Rácz", null, Equality.EQUAL, "Debreczy", null);

    assertAuth("White, Herbert & Harvey", null, Equality.EQUAL, "A.J. White, Herbert et P.J. Harvey", null);
    assertAuth("A.J.White", null, Equality.EQUAL, "A.J. White, Herbert et P.J. Harvey", null);
    assertAuth("Harvey", null, Equality.EQUAL, "A.J. White, Herbert et P.J. Harvey", null);

    assertAuth("R.H.Roberts", null, Equality.DIFFERENT, "R.J.Roberts", null);
    assertAuth("V.J.Chapm.", null, Equality.DIFFERENT, "F.R.Chapm.", null);
    assertAuth("V.J.Chapm.", null, Equality.DIFFERENT, "F.Chapm.", null);
    assertAuth("Chapm.", null, Equality.EQUAL, "F.R.Chapm.", null);
    assertAuth("Chapm.", null, Equality.EQUAL, "A.W.Chapm.", null);

    assertAuth("Brot. ex Willk. & Lange", null, Equality.DIFFERENT, "L.", null);

    assertAuth("Brugg.", null, Equality.EQUAL, "Brug.", null);
    assertAuth("A.Bruggen.", null, Equality.EQUAL, "Brug.", null);
    assertAuth("Brug.", null, Equality.EQUAL, "Pascal Bruggeman", null);

    assertAuth("Presl ex DC.", null, Equality.EQUAL, "C. Presl ex de Candolle", null);

    // https://github.com/gbif/checklistbank/issues/7
    assertAuth("G. Don f.", null, Equality.EQUAL, "G. Don fil.", null);
    assertAuth("Don f.", null, Equality.EQUAL, "Don fil.", null);
    assertAuth("F.K. Schimp. et Spenn.", null, Equality.EQUAL, "K.F. Schimp. et Spenn.", null);
    assertAuth("J.A. Weinm.", null, Equality.EQUAL, "Weinm.", null);
    assertAuth("DC. ex Lam. et DC.", null, Equality.EQUAL, "DC.", null);

    assertAuth("Koch", null, Equality.EQUAL, "Johann Friedrich Wilhelm Koch", null);
    assertAuth("Koch", null, Equality.EQUAL, "J F W Koch", null);
    assertAuth("Koch", null, Equality.EQUAL, "H Koch", null);

    assertAuth("L.f", null, Equality.EQUAL, "Linnaeus filius", null);
    assertAuth("L. f", null, Equality.EQUAL, "Linnaeus filius", null);
    assertAuth("L.fil.", null, Equality.EQUAL, "Linnaeus filius", null);

    assertAuth("Schultz-Bip", null, Equality.EQUAL, "Sch.Bip.", null);

    assertAuth("Bruand", "1850", Equality.EQUAL, "Bruand", "1851");
    assertAuth("Bruand", "1850", Equality.DIFFERENT, null, "1998");
    assertAuth("Bruand", "1850", Equality.EQUAL, null, "1850");
    assertAuth("Bruand", "1850", Equality.EQUAL, null, "1851");
  }

  @Test
  public void testCompareStrict() throws Exception {
    assertFalse(comp.compareStrict(null, null, null, null));
    assertFalse(comp.compareStrict("", "", "", ""));
    assertFalse(comp.compareStrict("", null, "", null));
    assertAuthStrict("", "  ", false, " ", "   ");
    assertAuthStrict("L.", null, false, null, null);

    assertAuthStrict("Bluff & Fingerh.", null, false, "Lindl.", null);
    assertAuthStrict("Lindl.", null, true, "Lindl.", null);

    assertAuthStrict(null, "1978", false, null, "1934");
    assertAuthStrict(null, "1978", false, null, "1978");

    assertAuthStrict("H. Christ", null, false, "C. Chr.", null);
    assertAuthStrict("Reichenbach", "1837", false, "Abasicarpon Andrz. ex Rchb.", null);
    assertAuthStrict("Reichenbach", null, true, "Abasicarpon Andrz. ex Rchb.", null);
    assertAuthStrict("Reichenbach", "1837", true, "Abasicarpon Andrz. ex Rchb.", "1837");

    assertAuthStrict("Torr et Gray", null, true, "Torr. & A.Gray", null);

    assertAuthStrict("Boed.", null, true, "Boed.", null);
    assertAuthStrict("Boed.", null, false, "F.Boos", null);
    assertAuthStrict("Boed.", null, false, "Boott", null);
    assertAuthStrict("Boed.", null, false, "F.Bolus", null);
    assertAuthStrict("Boed.", null, false, "Borchs.", null);

    assertAuthStrict("Hett.", null, false, "Scheffers", null);
    assertAuthStrict("Hett.", null, false, "Schew.", null);
    assertAuthStrict("Hett.", null, false, "Schemmann", null);
    assertAuthStrict("Hett.", null, false, "W.Mast.", null);
    assertAuthStrict("Hett.", null, false, "Kirschst.", null);


    /**
     * http://dev.gbif.org/issues/browse/POR-398
     */
    assertTrue(comp.compareStrict("Ridgway", "1893", "Ridgway", "1893"));
    assertTrue(comp.compareStrict("K. Koch", null, "K. Koch", null));
    assertTrue(comp.compareStrict("K.Koch", null, "K. Koch", null));
    assertTrue(comp.compareStrict("A. Nelson", null, "A Nélson", null));
    assertTrue(comp.compareStrict("Colla", null, "Bertero ex Colla", null));
    assertTrue(comp.compareStrict("Taczanowski & Berlepsch", "1885", "Berlepsch & Taczanowski", "1884"));

    assertFalse(comp.compareStrict("Taczanowski & Berlepsch", "1885", "Berlepsch & Taczanowski", "1883"));
    assertFalse(comp.compareStrict("Oberholser", "1917", "Oberholser", "1919"));
    assertFalse(comp.compareStrict("Koch", "1897", "K. Koch", null));
    assertFalse(comp.compareStrict("Gould", "1860", "Gould", "1862"));
    assertFalse(comp.compareStrict("Gould", "1860", "Gould", "1863"));
    assertFalse(comp.compareStrict("A. Nelson", null, "E.E. Nelson", null));

    assertFalse(comp.compareStrict("Koch", null, "K. Koch", null));
    assertTrue(comp.compareStrict("J Koch", null, "Koch", null));
    assertTrue(comp.compareStrict("Taczanowski & Berlepsch", "1885", "Berlepsch & Taczanowski", "1885"));

    assertTrue(comp.compareStrict("J Koch", null, "Koch", null));

    assertFalse(comp.compareStrict("Chapm.", null, "F.R.Chapm.", null));
    assertTrue(comp.compareStrict("Chapm.", null, "A.W.Chapm.", null));

    assertTrue(comp.compareStrict("Brugg.", null, "Brug.", null));
    assertFalse(comp.compareStrict("A.Bruggen.", null, "Brug.", null));
    assertTrue(comp.compareStrict("Brug.", null, "Pascal Bruggeman", null));
    assertTrue(comp.compareStrict("Koch", null, "Johann Friedrich Wilhelm Koch", null));
    assertTrue(comp.compareStrict("Koch", null, "J F W Koch", null));
    assertFalse(comp.compareStrict("Koch.", null, "H Koch", null));
  }

  @Test
  public void testEqualsWithBasionym() throws Exception {
    assertAuth("Maxim.", null, "Trautv. ex Maxim.", null, Equality.EQUAL, "Maxim.", null, null, null);
    assertAuth("Maxim.", null, "Trautv. ex Karl Johann Maximowicz", null, Equality.EQUAL, "Max.", null, null, null);
    assertAuth("Maxim.", null, null, null, Equality.EQUAL, "Karl Johann Maximowicz", null, null, null);

    assertAuth("Bluff & Fingerh.", null, "L.", null, Equality.DIFFERENT, "Mill.", "1768", null, null);
    assertAuth("Mill.", null, "L.", null, Equality.EQUAL, "Mill.", "1768", null, null);

    assertAuth("Debreczy & I. Rácz", null, null, null, Equality.EQUAL, "Debreczy & Rácz", null, null, null);
    assertAuth("Debreczy & I.Rácz", null, null, null, Equality.DIFFERENT, "Silba", null, "Debreczy & I.Rácz", null);

    assertAuth(null, null, "Pauly", "1986", Equality.EQUAL, null, null, "Pauly", "1986");
    assertAuth(null, null, "Moure", "1956", Equality.DIFFERENT, null, null, "Pauly", "1986");
    // missing brackets is a common error so make this a positive comparison!
    assertAuth("Pauly", "1986", null, null, Equality.EQUAL, null, null, "Pauly", "1986");

    assertAuth("Erichson", "1847", null, null, Equality.UNKNOWN, null, null, "Linnaeus", "1758");
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
   * see http://dev.gbif.org/issues/browse/PF-2445
   */
  @Test
  public void testTransliterations() throws Exception {
    ParsedName p1 = new ParsedName();
    p1.setAuthorsParsed(true);
    p1.setAuthorship("Müller");

    ParsedName p2 = new ParsedName();
    p2.setAuthorsParsed(true);
    p2.setAuthorship("Muller");

    ParsedName p3 = new ParsedName();
    p3.setAuthorsParsed(true);
    p3.setAuthorship("Mueller");

    assertEquals(Equality.EQUAL, comp.compare(p1, p2));
    assertEquals(Equality.EQUAL, comp.compare(p1, p3));
    assertEquals(Equality.EQUAL, comp.compare(p2, p3));


    p1.setAuthorship("Müll.");
    p2.setAuthorship("Mull");
    p3.setAuthorship("Muell");

    assertEquals(Equality.EQUAL, comp.compare(p1, p2));
    assertEquals(Equality.EQUAL, comp.compare(p1, p3));
    assertEquals(Equality.EQUAL, comp.compare(p2, p3));
  }

  private void assertAuth(String a1, String y1, Equality eq, String a2, String y2) {
    assertEquals(eq, comp.compare(a1, y1, a2, y2));
  }

  private void assertAuthStrict(String a1, String y1, boolean eq, String a2, String y2) {
    assertEquals(eq, comp.compareStrict(a1, y1, a2, y2));
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
}