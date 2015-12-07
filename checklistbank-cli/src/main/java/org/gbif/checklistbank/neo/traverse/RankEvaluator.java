package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.NeoProperties;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

/**
 * Includes only paths with taxon end nodes that have a rank equal or above the threshold given.
 */
public class RankEvaluator implements Evaluator {

  private final Rank threshold;

  public RankEvaluator(Rank threshold) {
    this.threshold = threshold;
  }

  @Override
  public Evaluation evaluate(Path path) {
    Node end = path.endNode();
    Rank rank = Rank.values()[ (int) end.getProperty(NeoProperties.RANK, Rank.UNRANKED.ordinal())];
    return threshold.higherThan(rank) ? Evaluation.EXCLUDE_AND_CONTINUE : Evaluation.INCLUDE_AND_CONTINUE;
  }
}
