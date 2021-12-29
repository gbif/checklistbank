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
package org.gbif.checklistbank.lucene;

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

    TokenStream result = new ScientificNameNormalizerFilter(source, true, true);
    result = new LowerCaseFilter(result);

    return new TokenStreamComponents(source, result);
  }

}
