package org.gbif.nub.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public final class LuceneUtils {

  /**
   *
   * @param analyzer
   * @param text
   * @return
   */
  public static List<String> analyze(Analyzer analyzer, String text) {
    List<String> result = new ArrayList<String>();
    try {
      TokenStream stream  = analyzer.tokenStream(null, new StringReader(text));
      stream.reset();
      while (stream.incrementToken()) {
        result.add(stream.getAttribute(CharTermAttribute.class).toString());
      }
      stream.close();
    } catch (IOException e) {
      // not thrown b/c we're using a string reader...
      throw new RuntimeException(e);
    }
    return result;
  }

}