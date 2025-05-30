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
package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.NeoProperties;

import javax.annotation.Nullable;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

/**
 * Includes only paths with taxon end nodes that have a rank equal or above the threshold given.
 */
public class RankEvaluator implements Evaluator {

  private final @Nullable Rank threshold;

  public RankEvaluator(Rank threshold) {
    this.threshold = threshold;
  }

  @Override
  public Evaluation evaluate(Path path) {
    return evaluateNode(path.endNode()) ? Evaluation.INCLUDE_AND_CONTINUE : Evaluation.EXCLUDE_AND_CONTINUE;
  }

  /**
   * @return true if the satisfies the rank evaluator and should be included.
   */
  public boolean evaluateNode(Node n) {
    if (threshold == null) return true;
    Rank rank = Rank.values()[ (int) n.getProperty(NeoProperties.RANK, Rank.UNRANKED.ordinal())];
    return !threshold.higherThan(rank);
  }
}
