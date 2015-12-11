package org.gbif.checklistbank.neo;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;

import org.neo4j.graphdb.Node;

/**
 * Property names of neo4j nodes.
 * Any property we store in neo should be listed here to avoid overlaps or other confusion.
 */
public class NeoProperties {
  // properties used in NeoTaxon
  public static final String TAXON_ID = DwcTerm.taxonID.simpleName();
  public static final String RANK = "rank";
  public static final String SCIENTIFIC_NAME = DwcTerm.scientificName.simpleName();
  public static final String CANONICAL_NAME = GbifTerm.canonicalName.simpleName();
  // used for proparte relations
  public static final String USAGE_KEY = "usageKey";
  public static final String NULL_NAME = "???";

  private NeoProperties() {
  }

  /**
   * @return the canonical name if existing, otherwise the scientific name property or ultimately the default ??? as last resort
   */
  public static String getCanonicalName(Node n) {
    if (n.hasProperty(NeoProperties.CANONICAL_NAME)) {
      return (String)n.getProperty(NeoProperties.CANONICAL_NAME);
    }
    return (String)n.getProperty(NeoProperties.SCIENTIFIC_NAME, NULL_NAME);
  }

  public static String getScientificName(Node n) {
    return (String)n.getProperty(NeoProperties.SCIENTIFIC_NAME, NULL_NAME);
  }
}
