package org.gbif.nub.lookup.fuzzy;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;

/**
 * Keyword analyzer that folds text to lower case ascii and finally applies a scientific name sound alike filter.
 */
public class ScientificNameAnalyzer extends Analyzer {

  public static final int BUFFER_SIZE = 1024;

  public ScientificNameAnalyzer() {
  }

  @Override
  protected TokenStreamComponents createComponents(final String fieldName) {
    KeywordTokenizer source = new KeywordTokenizer(BUFFER_SIZE);

    TokenStream result = new ASCIIFoldingFilter(source);
    result = new LowerCaseFilter(result);
    result = new ScientificNameSoundAlikeFilter(result);

    return new TokenStreamComponents(source, result);
  }

}
