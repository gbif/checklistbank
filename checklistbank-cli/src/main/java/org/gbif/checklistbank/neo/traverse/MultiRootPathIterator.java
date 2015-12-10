package org.gbif.checklistbank.neo.traverse;

import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.traversal.TraversalDescription;

/**
 * Path iterator that traverses multiple start nodes in a given traversal description.
 */
public class MultiRootPathIterator extends MultiRooIterator<Path> {

  private final TraversalDescription td;

  private MultiRootPathIterator(List<Node> roots, TraversalDescription td) {
    super(roots);
    this.td = td;
    prefetch();
  }

  public static Iterable<Path> create(final List<Node> roots, final TraversalDescription td) {
    return new Iterable<Path>() {
      @Override
      public Iterator<Path> iterator() {
        return new MultiRootPathIterator(roots, td);
      }
    };
  }

  @Override
  ResourceIterator<Path> iterateRoot(Node root) {
    return td.traverse(root).iterator();
  }

}
