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
package org.gbif.checklistbank.neo;

import org.gbif.api.vocabulary.Rank;
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

  public static Rank getRank(Node n, Rank defaultValue) {
    if (n.hasProperty(NeoProperties.RANK)) {
      return Rank.values()[(int)n.getProperty(NeoProperties.RANK)];
    }
    return defaultValue;
  }

  public static String getScientificName(Node n) {
    return (String)n.getProperty(NeoProperties.SCIENTIFIC_NAME, NULL_NAME);
  }
}
