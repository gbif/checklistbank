package org.gbif.checklistbank.neo.traverse;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.traversal.TraversalDescription;

/**
 * Path iterator that traverses multiple start nodes in a given traversal description.
 */
public class MultiRootNodeIterator extends MultiRooIterator<Node> {

  private final TraversalDescription td;

  private MultiRootNodeIterator(List<Node> roots, TraversalDescription td) {
    super(roots);
    this.td = td;
    prefetch();
  }

  public static Iterable<Node> create(final Node root, final TraversalDescription td) {
    return create(Lists.newArrayList(root), td);
  }

  public static Iterable<Node> create(final List<Node> roots, final TraversalDescription td) {
    return new Iterable<Node>() {
      @Override
      public Iterator<Node> iterator() {
        return new MultiRootNodeIterator(roots, td);
      }
    };
  }

  @Override
  ResourceIterator<Node> iterateRoot(Node root) {
    return td.traverse(root).nodes().iterator();
  }

}
