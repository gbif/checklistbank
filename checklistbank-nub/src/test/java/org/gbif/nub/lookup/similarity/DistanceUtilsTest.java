package org.gbif.nub.lookup.similarity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
