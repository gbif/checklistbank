package org.gbif.checklistbank.index.filter;


import java.io.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ScientificNameAnalyzerTest {

  ScientificNameAnalyzer analyzer = new ScientificNameAnalyzer();

  @Test
  public void testAnalyzer() throws Exception {
    assertAnalyzer(null, "");
    assertAnalyzer("abies", "Abies ");
    assertAnalyzer("abies", "Abiies ");
    assertAnalyzer("abyes", "Abyes ");
    assertAnalyzer("abyes alba", "Abyes  albus");
    assertAnalyzer("abyes albieta", "Abyes albieta");
    assertAnalyzer("abies albieta", "Abies albijeta");
    assertAnalyzer("abies albieta", "Abies albyeta");
    assertAnalyzer("abies alba", " \txAbies × ållbbus\t");
    assertAnalyzer("abies alba", " \txAbies × ållbbus\t");
    assertAnalyzer("rhachis taktos", "Rhachis taktos");
    assertAnalyzer("hieracium sabauda", "Hieracium sabaudum");
    assertAnalyzer("hieracium scorzoneraefolia", "Hieracium scorzoneræfolium");
    assertAnalyzer("hieracium scorzonerifolia", "Hieracium scorzonerifolium");
    assertAnalyzer("macrozamia platirachis", "Macrozamia  platyrachis");
    assertAnalyzer("macrozamia platirachis", "Macrozamia platyrhachis");
    assertAnalyzer("cycas circinalis", "Cycas circinalis");
    assertAnalyzer("cycas circinalis", "Cycas circinnalis");
    assertAnalyzer("isolona perieri", "Isolona perrieri");
    assertAnalyzer("isolona perieri", "Isolona perrierii");
    assertAnalyzer("carex caioueti", "Carex ×cayouettei");
    assertAnalyzer("platanus hispanica", "Platanus x hispanica");
    assertAnalyzer("eragrostis browni", "Eragrostis brownii");
    assertAnalyzer("eragrostis browni", "Eragrostis brownei");

    assertAnalyzer("eragrostis browni", "Eragrostis brownei");
  }

  void assertAnalyzer(String expected, String name) throws Exception {
    // use lucene analyzer to normalize input without using the full query parser
    StringBuilder sb = new StringBuilder();
    try (TokenStream stream = analyzer.tokenStream("anything", new StringReader(name))) {
      CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);
      stream.reset();
      while (stream.incrementToken()) {
        sb.append(termAtt.toString());
      }
      stream.end();
    }
    assertEquals(expected, expected);
  }
}