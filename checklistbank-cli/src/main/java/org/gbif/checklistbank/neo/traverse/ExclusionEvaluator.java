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
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.neo.NeoProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

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
