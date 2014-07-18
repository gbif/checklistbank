package org.gbif.checklistbank.neo;

import org.neo4j.graphdb.Label;

/**
 *
 */
public enum Labels implements Label {
  TAXON,
  ACCEPTED,
  SYNONYM,
  ROOT
}
