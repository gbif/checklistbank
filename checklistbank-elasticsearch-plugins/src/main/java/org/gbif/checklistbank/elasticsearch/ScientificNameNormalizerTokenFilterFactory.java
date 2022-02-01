/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.elasticsearch;

import org.gbif.checklistbank.lucene.ScientificNameNormalizerFilter;

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;
import org.elasticsearch.index.analysis.NormalizingTokenFilterFactory;

public class ScientificNameNormalizerTokenFilterFactory extends AbstractTokenFilterFactory implements
  NormalizingTokenFilterFactory {

  public static final String FULL_NAME_TOKENS_PARAM = "fullNameTokens";
  public static final String STEMMING_PARAM = "stemming";
  private final boolean fullNameTokens;
  private final boolean stemming;

  public ScientificNameNormalizerTokenFilterFactory(
    IndexSettings indexSettings, Environment environment, String name, Settings settings
  ) {
    super(indexSettings, name, settings);
    fullNameTokens = settings.getAsBoolean(FULL_NAME_TOKENS_PARAM, false);
    stemming = settings.getAsBoolean(STEMMING_PARAM, true);
  }

  @Override
  public TokenStream create(TokenStream tokenStream) {
    return new ScientificNameNormalizerFilter(tokenStream, fullNameTokens, stemming);
  }
}
