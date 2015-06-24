package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.NodeProperties;

import java.util.Comparator;

import org.neo4j.graphdb.Node;

/**
 * Orders taxon nodes by their rank first, then canonical name.
 */
public class TaxonOrder implements Comparator<Node> {

  @Override
  public int compare(Node n1, Node n2) {
    int r1 = (int) n1.getProperty(NodeProperties.RANK, 9999);
    int r2 = (int) n2.getProperty(NodeProperties.RANK, 9999);

    if (r1!=r2) {
      return r1 - r2;
    }
    return n1.getProperty(NodeProperties.CANONICAL_NAME, "").toString()
      .compareTo(n2.getProperty(NodeProperties.CANONICAL_NAME, "").toString());
  }
}
