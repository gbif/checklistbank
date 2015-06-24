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
  public static final String ORIGIN = "origin";
  public static final String SCIENTIFIC_NAME = DwcTerm.scientificName.simpleName();
  public static final String CANONICAL_NAME = GbifTerm.canonicalName.simpleName();

  // other id/verbatim relation values stored as neo node properties
  public static final String ACCEPTED_NAME_USAGE_ID = DwcTerm.acceptedNameUsageID.simpleName();
  public static final String ACCEPTED_NAME_USAGE = DwcTerm.acceptedNameUsage.simpleName();

  public static final String PARENT_NAME_USAGE_ID = DwcTerm.parentNameUsageID.simpleName();
  public static final String PARENT_NAME_USAGE = DwcTerm.parentNameUsage.simpleName();

  public static final String ORIGINAL_NAME_USAGE_ID = DwcTerm.originalNameUsageID.simpleName();
  public static final String ORIGINAL_NAME_USAGE = DwcTerm.originalNameUsage.simpleName();


  private NodeProperties() {
  }

}
