package org.gbif.nub.lookup.fuzzy;

import org.gbif.checklistbank.utils.SciNameNormalizer;

import java.io.IOException;

import com.google.common.base.Strings;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on character transpositions found in Tony Reese's TaxonMatch.
 */
public class ScientificNameNormalizerFilter extends TokenFilter {
  private static final Logger LOG = LoggerFactory.getLogger(ScientificNameNormalizerFilter.class);

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
    if (!Strings.isNullOrEmpty(term)) {
      String normed = SciNameNormalizer.normalize(term);
      termAtt.copyBuffer(normed.toCharArray(), 0, normed.length());
    }
    return true;
  }
}
