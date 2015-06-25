package org.gbif.checklistbank.neo;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;

/**
 * Property names of neo4j nodes.
 * Any property we store in neo should be listed here to avoid overlaps or other confusion.
 */
public class NodeProperties {
  // properties used in NeoTaxon
  public static final String TAXON_ID = DwcTerm.taxonID.simpleName();
  public static final String RANK = "rank";
  public static final String SCIENTIFIC_NAME = DwcTerm.scientificName.simpleName();
  public static final String CANONICAL_NAME = GbifTerm.canonicalName.simpleName();

  private NodeProperties() {
  }

}
