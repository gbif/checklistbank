package org.gbif.checklistbank.neo;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;

/**
 *
 */
public class TaxonProperties {
    public static final String TAXON_ID = DwcTerm.taxonID.simpleName();
    public static final String RANK = "rank";
    public static final String SCIENTIFIC_NAME = DwcTerm.scientificName.simpleName();
    public static final String CANONICAL_NAME = GbifTerm.canonicalName.simpleName();

    private TaxonProperties() {
    }

}
