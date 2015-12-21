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
