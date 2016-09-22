package org.gbif.checklistbank.index.filter;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 *
 */
public class ScientificNameNormalizerFilterFactory extends TokenFilterFactory {

  /**
   * Initialize this factory via a set of key-value pairs.
   */
  public ScientificNameNormalizerFilterFactory(Map<String, String> args) {
    super(args);
  }

  @Override
  public TokenStream create(TokenStream input) {
    return new ScientificNameNormalizerFilter(input);
  }
}
