package org.gbif.checklistbank.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Throwables;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 *
 */
public class LuceneUtils {

  private LuceneUtils() {}

  public static List<String> analyzeString(Analyzer analyzer, String string) {
    List<String> result = new ArrayList<String>();
    try (TokenStream stream  = analyzer.tokenStream(null, new StringReader(string))){
      stream.reset();
      while (stream.incrementToken()) {
        result.add(stream.getAttribute(CharTermAttribute.class).toString());
      }
    } catch (IOException e) {
      // not thrown b/c we're using a string reader...
      Throwables.propagate(e);
    }
    return result;
  }
}
