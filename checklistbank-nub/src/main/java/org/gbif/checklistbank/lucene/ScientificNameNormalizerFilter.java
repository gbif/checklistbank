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

import org.gbif.checklistbank.utils.SciNameNormalizer;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Lucene filter that uses the SciNameNormalizer util to normalize scientific names.
 * By default it strongly normalizes all name tokens as generated by the Standard- or WhitespaceTokenizer.
 *
 * When using it with tokens containing whitespace and representing entire scientific names as generated by the KeywordTokenizer
 * one should set fullNameTokens=true to keep monomials & genera mostly untouched.
 */
public class ScientificNameNormalizerFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final boolean fullNameTokens;
  private final boolean stemming;

  /**
   * Construct a token stream filtering the given input.
   */
  public ScientificNameNormalizerFilter(TokenStream input) {
    this(input, false, true);
  }

  /**
   * @param fullNameTokens if true the name normalization is done on the entire token. If false just on the epithets, leaving the genus part untouched
   * @param stemming if true gender stemming of tokens is done
   */
  public ScientificNameNormalizerFilter(TokenStream input, boolean fullNameTokens, boolean stemming) {
    super(input);
    this.fullNameTokens = fullNameTokens;
    this.stemming = stemming;
  }

  @Override
  public final boolean incrementToken() throws IOException {
    if (!input.incrementToken()) return false;

    String term = termAtt.toString();
    if (SciNameNormalizer.hasContent(term)) {

      String normed = fullNameTokens ? SciNameNormalizer.normalize(term, stemming) : SciNameNormalizer.normalizeAll(term, stemming);
      termAtt.copyBuffer(normed.toCharArray(), 0, normed.length());
    }
    return true;
  }

}
