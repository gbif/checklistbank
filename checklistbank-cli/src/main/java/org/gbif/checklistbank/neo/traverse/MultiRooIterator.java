package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.NeoProperties;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Ordering;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Path iterator that traverses multiple start nodes in a given traversal description.
 */
public abstract class MultiRooIterator<T> implements AutoCloseable, Iterator<T> {

  private static final Logger LOG = LoggerFactory.getLogger(MultiRooIterator.class);

  private final Iterator<Node> roots;
  private ResourceIterator<T> rootPaths;
  private T next;

  MultiRooIterator(List<Node> roots) {
    this.roots = Ordering.from(new TaxonomicOrder()).sortedCopy(roots).iterator();
    LOG.debug("Found {} root nodes to iterate over", roots.size());
  }

  @Override
  public boolean hasNext() {
    return next != null;
  }

  @Override
  public T next() {
    T p = next;
    prefetch();
    return p;
  }

  abstract ResourceIterator<T> iterateRoot(Node root);

  public void prefetch() {
    while ((rootPaths == null || !rootPaths.hasNext()) && roots.hasNext()) {
      // close as quickly as we can
      if (rootPaths != null) {
        rootPaths.close();
      }
      Node root = roots.next();
      LOG.debug("Traverse a new root taxon: {}", NeoProperties.getName(root));
      rootPaths = iterateRoot(root);
    }
    if (rootPaths != null && rootPaths.hasNext()) {
      next = rootPaths.next();
    } else {
      next = null;
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() throws Exception {
    if (rootPaths != null) {
      rootPaths.close();
    }
  }

}
