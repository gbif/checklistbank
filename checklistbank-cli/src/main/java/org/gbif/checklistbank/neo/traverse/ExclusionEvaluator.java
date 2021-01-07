package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.api.vocabulary.Rank;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Includes only paths that have accepted taxon end nodes.
 */
public class ExclusionEvaluator implements Evaluator {
  private final Map<String, Rank> exclusion = new HashMap<>();

  public ExclusionEvaluator (List<RankedName> exclusion){
    for (RankedName ru : exclusion) {
      this.exclusion.put(ru.name.toUpperCase().trim(), ru.rank);
    }
  }

  @Override
  public Evaluation evaluate(Path path) {
    Node end = path.endNode();
    String name = NeoProperties.getCanonicalName(end).toUpperCase().trim();
    if (exclusion.containsKey(name)) {
      Rank rank = NeoProperties.getRank(end, Rank.UNRANKED);
      if (rank == exclusion.get(name)) {
        return Evaluation.EXCLUDE_AND_PRUNE;
      }
    }
    return Evaluation.INCLUDE_AND_CONTINUE;
  }
}
