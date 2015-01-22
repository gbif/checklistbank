package org.gbif.nub.lookup;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;

public class ScientificNameAnalyzer extends Analyzer {

  public static final int BUFFER_SIZE = 1024;

  public ScientificNameAnalyzer() {
  }

  @Override
  protected TokenStreamComponents createComponents(final String fieldName, final Reader reader) {
    KeywordTokenizer source = new KeywordTokenizer(reader, BUFFER_SIZE);

    TokenStream result = new LowerCaseFilter(source);
    result = new ScientificNameSoundAlikeFilter(result);

    return new TokenStreamComponents(source, result);
  }

}
