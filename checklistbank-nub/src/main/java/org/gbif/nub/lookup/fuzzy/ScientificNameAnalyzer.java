package org.gbif.nub.lookup.fuzzy;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;

/**
 * Keyword analyzer that uses the scientific name normalizer
 */
public class ScientificNameAnalyzer extends Analyzer {

  public static final int BUFFER_SIZE = 1024;

  public ScientificNameAnalyzer() {
  }

  @Override
  protected TokenStreamComponents createComponents(final String fieldName) {
    KeywordTokenizer source = new KeywordTokenizer(BUFFER_SIZE);

    //TokenStream result = new ASCIIFoldingFilter(source);
    //result = new ScientificNameSoundAlikeFilter(result);

    TokenStream result = new ScientificNameNormalizerFilter(source);
    result = new LowerCaseFilter(result);

    return new TokenStreamComponents(source, result);
  }
}
