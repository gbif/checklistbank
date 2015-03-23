package org.gbif.nub.lookup.similarity;

/**
 * Interface for string similarities.
 */
public interface StringSimilarity {

  /**
   * @return between 0 and 100 based on how similar the specified strings are to one another, 100 being the perfect match
   */
  double getSimilarity(String x1, String x2);

}
