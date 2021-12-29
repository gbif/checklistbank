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

import org.gbif.checklistbank.utils.SciNameNormalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apply normalizations to scientific names before scoring them for similarity
 * using edit distance applied to each epithet.
 */
public class ScientificNameSimilarity implements StringSimilarity {

  private static final Logger LOG = LoggerFactory.getLogger(ScientificNameSimilarity.class);

  private static final int MUST_MATCH = 4;

  ModifiedDamerauLevenshtein mdl1 = new ModifiedDamerauLevenshtein(1);
  ModifiedDamerauLevenshtein mdl3 = new ModifiedDamerauLevenshtein(3);

  @Override
  public double getSimilarity(String x1, String x2) {
    if (x1.equals(x2)) return 100d;

    LOG.debug("‘{}’\twas previously {}% like ‘{}’", x1, mdl3.getSimilarity(x1, x2), x2);

    x1 = SciNameNormalizer.normalize(x1);
    x2 = SciNameNormalizer.normalize(x2);

    String[] x1s = x1.split(" ");
    String[] x2s = x2.split(" ");

    // Compare the whole name if they don't have the same number of tokens.
    if (x1s.length != x2s.length) {
      double sim = mdl3.getSimilarity(x1, x2);
      LOG.debug("‘{}’\tis {}% like ‘{}’ (but lengths differ)\n", x1, sim, x2);
      return sim;
    }

    boolean bad = false;
    double overallSim = 0;
    int i;
    for (i = 0; i < x1s.length; i++) {
      double sim = similarity(x1s[i], x2s[i]);
      // The score of the first epithet (e.g. genus) is scaled down, 100→100, 50→0, <50→0
      if (i == 0) {
        sim = Math.max(0, (2*sim - 100));
      }
      overallSim += sim;

      if (sim == 0) bad = true;
    }
    overallSim = overallSim/i;

    // Any epithet that doesn't match enough makes the whole match bad.
    if (bad && overallSim > 5) {
      overallSim = 5;
    }

    LOG.debug("‘{}’\tis {}% like ‘{}’\n", x1, overallSim, x2);
    return overallSim;
  }

  private double similarity(String x1, String x2) {
    // First letter much match
    if (x1.charAt(0) != x2.charAt(0)) {
      LOG.debug("\t‘{}’\tis not at all like ‘{}’ (‘{}’≠‘{}’)", x1, x2, x1.charAt(0), x2.charAt(0));
      return 0;
    }

    // Very short epithets must match exactly
    if (x1.length() < MUST_MATCH || x2.length() < MUST_MATCH) {
      if (x1.equals(x2)) {
        LOG.debug("\t‘{}’\tis short and exactly like ‘{}’", x1, x2);
        return 100;
      } else {
        LOG.debug("\t‘{}’\tis short and nothing like ‘{}’", x1, x2);
        return 0;
      }
    }

    // Longer ones can have one change in the first MUST_MATCH letters
    // TODO: Consider whether the first letters must match exactly.
    String x1head = x1.substring(0, MUST_MATCH);
    String x2head = x2.substring(0, MUST_MATCH);
    int dist;
    if ((dist = mdl1.getEditDistance(x1head, x2head)) > 1) {
      LOG.debug("\t‘{}’\tis nothing like ‘{}’ (‘{}’≠‘{}’, dist={})", x1, x2, x1head, x2head, dist);
      return 0;
    }

    // And up to two changes in the whole epithet
    // TODO: Use Markus’ distance utility thing to take account of length.
    dist = mdl1.getEditDistance(x1, x2);
    double r = (dist == 0 ? 100 : (dist == 1 ? 90 : (dist <= 2 ? 80 : 0)));

    LOG.debug("\t‘{}’\tis {}% like ‘{}’", x1, r, x2);
    return r;
  }
//
//
//
}
