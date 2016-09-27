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
  private final boolean fullNameTokens;

  public ScientificNameNormalizerFilterFactory(Map<String, String> args) {
    super(args);
    fullNameTokens = getBoolean(args, FULL_NAME_TOKENS_PARAM, false);
  }

  @Override
  public TokenStream create(TokenStream input) {
    return new ScientificNameNormalizerFilter(input, fullNameTokens);
  }
}
