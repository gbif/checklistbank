package org.gbif.checklistbank.nub;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.checklistbank.neo.traverse.TreeWalker;

import java.util.Map;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does some basic verifications on the neo4j nub tree
 * This does not rely on name usages or metrics to exist yet!
 */
public class NubTreeValidation implements TreeValidation {
  private static final Logger LOG = LoggerFactory.getLogger(NubTreeValidation.class);

  private final NubDb db;
  private boolean valid = true;

  public NubTreeValidation(NubDb db) {
    this.db = db;
  }

  private Result query(String cypher) {
    LOG.debug("Execute: {}", cypher);
    return db.dao.getNeo().execute(cypher);
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
    try {
      TreeRankValidation rankVal = new TreeRankValidation();
      TreeWalker.walkAcceptedTree(db.dao.getNeo(), rankVal);
    } catch (IllegalStateException e) {
      LOG.error("TreeRankValidation failed with {}", e.getMessage());
      valid = false;
    }

    return valid;
  }
}
