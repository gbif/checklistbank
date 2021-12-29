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
package org.gbif.nub.lookup.similarity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScientificNameSimilarityTest {

  @Test
  public void testSimilarities() throws Exception {
    ScientificNameSimilarity sns = new ScientificNameSimilarity();

    // The × in these ensures they don't match the shortcut .equals test.
    assertEquals(100d, sns.getSimilarity("A", "×A"), 0.01d);
    assertEquals(100d, sns.getSimilarity("Aa", "×Aa"), 0.01d);
    assertEquals(100d, sns.getSimilarity("Io", "×Io"), 0.01d);
    assertEquals(100d, sns.getSimilarity("Aus", "×Aus"), 0.01d);
    assertEquals(100d, sns.getSimilarity("Ausbus", "×Ausbus"), 0.01d);
    assertEquals(0d, sns.getSimilarity("Aus", "×Ausausaus"), 0.01d);
    assertEquals(0d, sns.getSimilarity("Ausausaus", "×Aus"), 0.01d);

    assertEquals(100d, sns.getSimilarity("abcdefg", "abcdefg"), 0.01d);
    assertEquals(80d, sns.getSimilarity("abcdefg", "amcdefg"), 0.01d);
    assertEquals(80d, sns.getSimilarity("abcdefg", "abcdeg"), 0.01d);

    assertEquals(0d, sns.getSimilarity("abcdefg", "zyxvwu"), 0.01d);
    assertEquals(0d, sns.getSimilarity("abcdefg", "amncdefg"), 0.01d);
    assertEquals(0d, sns.getSimilarity("abcdefg", "adefg"), 0.01d);
    assertEquals(0d, sns.getSimilarity("abcdefg", "aabbccddeeffgg"), 0.01d);
    assertEquals(100d, sns.getSimilarity("Aka abcdefg", "Aka aabbccddeeffgg"), 0.01d);
    assertEquals(100d, sns.getSimilarity("äöüæøåœđł", "aouaeoaoedl"), 0.01d);

    assertEquals(100d, sns.getSimilarity("xAus", "Aus"), 0.01d);
    assertEquals(100d, sns.getSimilarity("x Aus", "Aus"), 0.01d);
    assertEquals(100d, sns.getSimilarity("×Aus", "Aus"), 0.01d);
    assertEquals(100d, sns.getSimilarity("× Aus", "Aus"), 0.01d);

    assertEquals(100d, sns.getSimilarity("Abies alba", "Abies alba"), 0.01d);
    assertEquals(100d, sns.getSimilarity("Abies alba", "Abies albus"), 0.01d);
    assertEquals(5d, sns.getSimilarity("Abies alba", "Abies olba"), 0.01d);
    assertEquals(95d, sns.getSimilarity("Abies alba", "Abies alta"), 0.01d);

    assertEquals(100d, sns.getSimilarity("Abies ama", "Abies amus"), 0.01d);
    assertEquals(100d, sns.getSimilarity("Abies ama", "Abies amus"), 0.01d);
    assertEquals(100d, sns.getSimilarity("Abies ama", "Abies amum"), 0.01d);

    assertEquals(95d, sns.getSimilarity("Linaria pedunculata", "Linaria pedinculata"), 0.01d);
    assertEquals(90d, sns.getSimilarity("Linaria pedunculata", "Lunaria pedunculata"), 0.01d);
    assertEquals(85d, sns.getSimilarity("Linaria pedunculata", "Linariya pedonculata"), 0.01d);
    assertEquals(93.33d, sns.getSimilarity("Linaria pedunculata vulgaris", "Lunaria pedunculata vulgaris"), 0.01d);
    assertEquals(5d, sns.getSimilarity("Linaria pedunculata vulgaris", "Linaria pedunculata vandalis"), 0.01d);
    assertEquals(5d, sns.getSimilarity("Oreina elegans", "Orfelia elegans"), 0.01d);
    assertEquals(5d, sns.getSimilarity("Lucina scotti", "Lucina wattsi"), 0.01d);
    assertEquals(0d, sns.getSimilarity("scotti", "wattsi"), 0.01d);
  }
}