package org.gbif.checklistbank.neo.printer;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 *
 */
public interface NeoPrinter extends AutoCloseable {

  public void printNodes(Iterable<Node> nodes);

  public void printEdges(Iterable<Relationship> relations);
}
