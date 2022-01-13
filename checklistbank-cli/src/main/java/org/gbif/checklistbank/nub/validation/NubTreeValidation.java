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
package org.gbif.checklistbank.nub.validation;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.nub.NubDb;

import java.util.Map;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does some basic verifications on the neo4j nub tree
 * This does not rely on name usages or metrics to exist yet!
 */
public class NubTreeValidation implements NubValidation {
  private static final Logger LOG = LoggerFactory.getLogger(NubTreeValidation.class);

  private final NubDb db;
  private boolean valid = true;

  public NubTreeValidation(NubDb db) {
    this.db = db;
  }

  private Result query(String cypher) {
    LOG.debug("Execute: {}", cypher);
    return db.dao().getNeo().execute(cypher);
  }

  private void notExists(String errorMessage, String cypher) {
    try (Transaction tx = db.beginTx()) {
      Result res = query(cypher);
      if (res.hasNext()) {
        valid = false;
        Map<String, Object> row = res.next();
        LOG.error(errorMessage, row);
      }
    }
  }

  private void assertCount(String errorMessage, String cypher, long expected) {
    try (Transaction tx = db.beginTx()) {
      Result res = query(cypher);
      Map<String, Object> row = res.next();
      long count = (long) row.values().iterator().next();
      if (expected != count) {
        valid = false;
        LOG.error(errorMessage, count);
      }
    }
  }

  @Override
  public boolean validate() {
    // no self loops whatsoever
    notExists("Self loops in {}", "MATCH (n:TAXON) WHERE (n)--(n) RETURN n LIMIT 1");

    // no parent_of rels in different directions on a given node
    notExists("Contradicting parent relations in {}", "MATCH (n:TAXON) WHERE (n)-[:PARENT_OF*..6]->(n) RETURN n LIMIT 1");

    // just kingdom roots
    assertCount("Too many roots {}", "MATCH (n:ROOT) RETURN count(n)", Kingdom.values().length);

    // no basionyms without basionym relation
    notExists("Basionym without basionym relation {}", "MATCH (b:BASIONYM) WHERE not (b)-[:BASIONYM_OF]->() RETURN b LIMIT 1");

    // no basionym exists without an accepted or parent node
    notExists("Orphaned basionym found {}", "MATCH (b:BASIONYM) WHERE not (b)-[:SYNONYM_OF]->() and not (b)<-[:PARENT_OF]-() RETURN b LIMIT 1");

    // no synonyms without synonym relation
    notExists("Synonym without synonym relation {}", "MATCH (s:SYNONYM) WHERE not (s)-[:SYNONYM_OF]->() RETURN s LIMIT 1");

    // verify accepted tree has ranks in proper order
    validateRanks();

    return valid;
  }

  private void validateRanks() {
    try (Transaction tx = db.beginTx()) {
      for (Node c : Iterators.loop(db.dao().getNeo().findNodes(Labels.ROOT))) {
        traverse(c, NeoProperties.getRank(c, null));
      }
    }
  }

  private void traverse(Node p, Rank pr) {
    for (Node c : Traversals.CHILDREN.traverse(p).nodes()) {
      Rank cr = NeoProperties.getRank(c, null);
      if (cr == null) {
        LOG.error("Missing rank: {}", NeoProperties.getScientificName(c));
        valid = false;

      } else if (!pr.higherThan(cr)) {
        LOG.error("Rank mismatch: {} {} CHILD OF {} {}", cr, NeoProperties.getScientificName(c), pr, NeoProperties.getScientificName(p));
        valid = false;
      }

      traverse(c, cr);
    }
  }

}
