package org.gbif.nub.utils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;

/**
 * Analyzer that uses a keyword tokenizer together with a AsciiFoldingFilter to transform strings.
 */
public class AsciiKeywordAnalyzer extends Analyzer {

  public AsciiKeywordAnalyzer() {
  }

  @Override
  protected TokenStreamComponents createComponents(final String fieldName) {
    KeywordTokenizer source = new KeywordTokenizer();

    TokenStream result = new ASCIIFoldingFilter(source);

    return new TokenStreamComponents(source, result);
  }

}
