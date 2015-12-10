package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.NeoProperties;

import java.util.Comparator;

import org.neo4j.graphdb.Node;

/**
 * Orders taxon nodes by their rank first, then canonical name.
 */
public class TaxonomicOrder implements Comparator<Node> {

  @Override
  public int compare(Node n1, Node n2) {
    int r1 = (int) n1.getProperty(NeoProperties.RANK, Integer.MAX_VALUE);
    int r2 = (int) n2.getProperty(NeoProperties.RANK, Integer.MAX_VALUE);

    if (r1!=r2) {
      return r1 - r2;
    }
    return NeoProperties.getName(n1).compareTo(NeoProperties.getName(n2));
  }

}
