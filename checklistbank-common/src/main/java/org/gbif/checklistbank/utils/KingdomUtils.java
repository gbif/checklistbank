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
package org.gbif.checklistbank.utils;

import org.gbif.api.vocabulary.Kingdom;

import java.util.Arrays;
import java.util.Set;

import com.google.common.collect.Sets;

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
