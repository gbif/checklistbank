package org.gbif.checklistbank.neo;

import org.neo4j.graphdb.Label;

/**
 *
 */
public enum Labels implements Label {
    TAXON,
    SYNONYM,
    BASIONYM,
    FAMILY,
    GENUS,
    SPECIES,
    INFRASPECIES,
    ROOT
}
