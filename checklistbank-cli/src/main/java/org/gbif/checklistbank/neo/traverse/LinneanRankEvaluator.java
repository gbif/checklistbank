package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.NeoProperties;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

/**
 * Includes only nodes that have Linnean ranks.
 */
public class LinneanRankEvaluator implements Evaluator {

  @Override
  public Evaluation evaluate(Path path) {
    Node end = path.endNode();
    Rank r = Rank.values()[(int) end.getProperty(NeoProperties.RANK, Rank.UNRANKED.ordinal())];
    return r.isLinnean() ? Evaluation.INCLUDE_AND_CONTINUE : Evaluation.EXCLUDE_AND_CONTINUE;
  }
}
