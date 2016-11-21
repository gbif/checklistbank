package org.gbif.checklistbank.lucene;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * Factory to be used in solr schemas to create a lucene ScientificNameNormalizerFilter.
 * If the argument fullNameTokens=true exists the normalizer will treat tokens as full scientific names and not parts of it.
 */
public class ScientificNameNormalizerFilterFactory extends TokenFilterFactory {

  public static final String FULL_NAME_TOKENS_PARAM = "fullNameTokens";
  public static final String STEMMING_PARAM = "stemming";
  private final boolean fullNameTokens;
  private final boolean stemming;

  public ScientificNameNormalizerFilterFactory(Map<String, String> args) {
    super(args);
    fullNameTokens = getBoolean(args, FULL_NAME_TOKENS_PARAM, false);
    stemming = getBoolean(args, STEMMING_PARAM, true);
  }

  @Override
  public TokenStream create(TokenStream input) {
    return new ScientificNameNormalizerFilter(input, fullNameTokens, stemming);
  }
}
