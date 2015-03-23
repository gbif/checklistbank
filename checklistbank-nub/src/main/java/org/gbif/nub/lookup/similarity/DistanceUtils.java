package org.gbif.nub.lookup.similarity;

public class DistanceUtils {

  public static double convertEditDistanceToSimilarity(int edits, String s1, String s2) {
    int length = Math.min(10, Math.min(s1.length(), s2.length())) ;
    double dist =  Math.pow(edits, 1.4) / (double) length;
    double sim = edits > length || dist > 1.0d ? 0 : 100d * (1d - dist);
    return Math.round(sim);
  }
}
