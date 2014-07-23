package org.gbif.checklistbank.neo.traverse;

import org.neo4j.graphdb.Node;

/**
 * An event handler interface that accepts a start and end event for a neo node.
 * Used in taxonomic traversals to implement workers for a classification walk.
 */
public interface StartEndHandler {

  void start(Node n);

  void end(Node n);
}
