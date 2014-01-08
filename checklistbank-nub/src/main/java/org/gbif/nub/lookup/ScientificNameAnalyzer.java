package org.gbif.nub.lookup;

import org.gbif.nub.utils.Constants;

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

    TokenStream result = new LowerCaseFilter(Constants.LUCENE_VERSION, source);
    result = new ScientificNameSoundAlikeFilter(result);

    return new TokenStreamComponents(source, result);
  }

}
