package org.gbif.checklistbank.utils;

import com.google.common.collect.Sets;
import org.gbif.api.vocabulary.Kingdom;

import java.util.Arrays;
import java.util.Set;

import static org.gbif.api.vocabulary.Kingdom.*;

/**
 *
 */
public class KingdomUtils {
  // matrix to do the quick comparison of alike kingdoms, see match(k1, k2);
  private static final boolean[][] KINGDOM_MATRIX;

  static {
    KINGDOM_MATRIX = new boolean[Kingdom.values().length][Kingdom.values().length];
    for (Kingdom k1 : Kingdom.values()) {
      for (Kingdom k2 : Kingdom.values()) {
        KINGDOM_MATRIX[k1.ordinal()][k2.ordinal()] = !Sets.intersection(expand(k1), expand(k2)).isEmpty();
      }
    }
  }

  private static Set<Kingdom> expand(Kingdom k) {
    switch (k) {
      case INCERTAE_SEDIS:
        return Sets.newHashSet(Kingdom.values());
      case PROTOZOA:
        return Sets.newHashSet(PROTOZOA, ANIMALIA, FUNGI, PLANTAE);
      case CHROMISTA:
        return Sets.newHashSet(CHROMISTA, ANIMALIA, FUNGI, PLANTAE);
      case ARCHAEA:
        return Sets.newHashSet(ARCHAEA, BACTERIA);
      default:
        return Sets.newHashSet(k);
    }
  }

  /**
   * Losely compare kingdoms by strictly keeping Animalia, Plantae, Fungi, Bacteria and Viruses distinct.
   * INCERTAE_SEDIS matches anything
   * Protozoa matches Animalia, Fungi & Plantae
   * Chromista matches Animalia, Fungi & Plantae
   * Archaea matches Bacteria
   *
   * @param k1
   * @param k2
   * @return
   */
  public static boolean match(Kingdom k1, Kingdom k2) {
    return KINGDOM_MATRIX[k1.ordinal()][k2.ordinal()];
  }

  static void logMatrix() {
    for (boolean[] row : KINGDOM_MATRIX) {
      System.out.println(Arrays.toString(row));
    }
  }

}
