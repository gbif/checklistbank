package org.gbif.checklistbank.neo;

import org.neo4j.graphdb.RelationshipType;

/**
 *
 */
public enum RelType implements RelationshipType {
  PARENT_OF,
  SYNONYM_OF,
  PROPARTE_SYNONYM_OF,
  BASIONYM_OF
}
