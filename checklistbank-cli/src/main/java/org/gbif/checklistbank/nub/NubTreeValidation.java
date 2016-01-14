package org.gbif.checklistbank.nub;

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

  @Override
  public boolean validate() {
    // no self loops whatsoever
    notExists("Self loops in {}", "MATCH (n:TAXON) WHERE (n)--(n) RETURN n LIMIT 1");

    // just kingdom roots

    // no basionym exists without an accepted or parent node
    notExists("Orphaned basionym found {}", "MATCH (b:TAXON) WHERE (b)<-[:BASIONYM_OF]-() and not (b)-[:SYNONYM_OF]->() and not (b)<-[:PARENT_OF]-() RETURN b LIMIT 1");

    // no synonyms without synonym relation
    notExists("Synonym without synonym relation {}", "MATCH (s:SYNONYM) WHERE not (s)-[:SYNONYM_OF]->() RETURN s LIMIT 1");

    // no basionyms without basionym relation
    notExists("Basionym without basionym relation {}", "MATCH (b:BASIONYM) WHERE not (b)<-[:BASIONYM_OF]-() RETURN b LIMIT 1");

    // TODO: accepted tree has ranks in proper order

    return valid;
  }
}
