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

public class DistanceUtilsTest {
  final double delta = 0.01d;

  @Test
  public void testConvertEditDistanceToSimilarity() throws Exception {
    assertEquals(100d, DistanceUtils.convertEditDistanceToSimilarity(0, "1234567890", "1234567890"), delta);
    assertEquals(67.0d, DistanceUtils.convertEditDistanceToSimilarity(2, "12345678", "12345678"), delta);
    assertEquals(42.0d, DistanceUtils.convertEditDistanceToSimilarity(3, "12345678", "12345678"), delta);
    assertEquals(86.0d, DistanceUtils.convertEditDistanceToSimilarity(1, "1234567", "123456789"), delta);
    // long strings make no difference
    assertEquals(90.0d, DistanceUtils.convertEditDistanceToSimilarity(1, "12345678901234", "1234567890x234"), delta);
    assertEquals(74.0d, DistanceUtils.convertEditDistanceToSimilarity(2, "12345678901234", "1234567890x234"), delta);
    assertEquals(53.0d, DistanceUtils.convertEditDistanceToSimilarity(3, "12345678901234", "1234567890x234"), delta);
    // doesnt make sense, but an edit distance of zero is always 100%
    assertEquals(100d, DistanceUtils.convertEditDistanceToSimilarity(0, "12", "123456789"), delta);
    assertEquals(0d, DistanceUtils.convertEditDistanceToSimilarity(10, "1234567", "123456789"), delta);
  }
}
