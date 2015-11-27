package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

/**
 * Marks appropriate points in the taxonomic tree where concurrent processing can start.
 * At present, families are marked. This could be improved based on the checklist,
 * or could take account of the current depth (path.length()), to avoid marking many
 * unplaced families.
 */
public class ChunkingEvaluator implements Evaluator {

  @Override
  public Evaluation evaluate(Path path) {
    Node end = path.endNode();

    int rank = (int)end.getProperty(NeoProperties.RANK, -1); // Unknown ranks should be passed, don't mark for a new thread.

    if (rank >= Rank.FAMILY.ordinal()) {
      end.addLabel(Labels.CHUNK);
      return Evaluation.INCLUDE_AND_PRUNE;
    }
    else {
      return Evaluation.INCLUDE_AND_CONTINUE;
    }
  }
}
