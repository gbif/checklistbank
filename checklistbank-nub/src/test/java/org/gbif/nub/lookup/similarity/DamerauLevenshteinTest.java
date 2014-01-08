package org.gbif.nub.lookup.similarity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DamerauLevenshteinTest {
  final double delta = 0.01d;

  @Test
  public void testConvertEditDistanceToSimilarity() throws Exception {
    assertEquals(100d, DamerauLevenshtein.convertEditDistanceToSimilarity(0, "1234567890", "1234567890"), delta);
    assertEquals(75d, DamerauLevenshtein.convertEditDistanceToSimilarity(2, "12345678", "12345678"), delta);
    assertEquals(87.5d, DamerauLevenshtein.convertEditDistanceToSimilarity(1, "1234567", "123456789"), delta);
    // doesnt make sense, but an edit distance of zero is always 100%
    assertEquals(100d, DamerauLevenshtein.convertEditDistanceToSimilarity(0, "12", "123456789"), delta);
    assertEquals(0d, DamerauLevenshtein.convertEditDistanceToSimilarity(10, "1234567", "123456789"), delta);
  }
}
