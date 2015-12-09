package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.NeoProperties;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Path iterator that traverses multiple start nodes in a given traversal description.
 */
public class MultiRootPathIterator implements AutoCloseable, Iterator<Path> {

  private static final Logger LOG = LoggerFactory.getLogger(MultiRootPathIterator.class);

  private final TraversalDescription td;
  private final LinkedList<Node> roots;
  private ResourceIterator<Path> rootPaths;
  private final String debugProperty;
  private Path next;

  private MultiRootPathIterator(List<Node> roots, TraversalDescription td, String debugProperty) {
    this.td = td;
    this.debugProperty = debugProperty;
    Collections.sort(roots, new TaxonOrder());
    this.roots = Lists.newLinkedList();
    this.roots.addAll(roots);
    LOG.debug("Found {} root nodes to iterate over", roots.size());
    prefetch();
  }

  public static Iterable<Path> create(final List<Node> roots, final TraversalDescription td, final String debugProperty) {
    return new Iterable<Path>() {
      @Override
      public Iterator<Path> iterator() {
        return new MultiRootPathIterator(roots, td, debugProperty);
      }
    };
  }

  public static Iterable<Path> create(final List<Node> roots, final TraversalDescription td) {
    return create(roots, td, NeoProperties.SCIENTIFIC_NAME);
  }

  @Override
  public boolean hasNext() {
    return next != null;
  }

  @Override
  public Path next() {
    Path p = next;
    prefetch();
    return p;
  }

  public void prefetch() {
    while ((rootPaths == null || !rootPaths.hasNext()) && !roots.isEmpty()) {
      // close as quickly as we can
      if (rootPaths != null) {
        rootPaths.close();
      }
      Node root = roots.removeFirst();
      LOG.debug("Traverse a new root taxon: {}", root.getProperty(debugProperty, null));
      rootPaths = td.traverse(root).iterator();
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
