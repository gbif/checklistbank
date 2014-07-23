package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.TaxonProperties;

import java.util.Comparator;

import org.neo4j.graphdb.Node;

/**
 * Orders taxon nodes by their scientific name.
 */
public class TaxonOrder implements Comparator<Node> {

  @Override
  public int compare(Node n1, Node n2) {
    return n1.getProperty(TaxonProperties.SCIENTIFIC_NAME, "")
      .toString()
      .compareTo(n2.getProperty(TaxonProperties.SCIENTIFIC_NAME, "").toString());
  }
}
