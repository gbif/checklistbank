package org.gbif.checklistbank.index.filter;

import org.gbif.checklistbank.utils.SciNameNormalizer;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Lucene filter that uses the SciNameNormalizer util to normalize scientific names.
 */
public class ScientificNameNormalizerFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  /**
   * Construct a token stream filtering the given input.
   */
  public ScientificNameNormalizerFilter(TokenStream input) {
    super(input);
  }

  @Override
  public final boolean incrementToken() throws IOException {
    if (!input.incrementToken()) return false;

    String term = termAtt.toString();
    if (term == null || term.trim().isEmpty()) {

      String normed = SciNameNormalizer.nullToEmpty(SciNameNormalizer.normalize(term));
      termAtt.copyBuffer(normed.toCharArray(), 0, normed.length());
    }
    return true;
  }
}
