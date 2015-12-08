package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.NeoProperties;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
  private final List<Node> roots;
  private ResourceIterator<Path> rootPaths;
  private final String debugProperty;

  private MultiRootPathIterator(List<Node> roots, TraversalDescription td, String debugProperty) {
    this.td = td;
    this.debugProperty = debugProperty;
    Collections.sort(roots, new TaxonOrder());
    this.roots = roots;
    LOG.debug("Found {} root nodes to iterate over", roots.size());
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
    return (rootPaths != null && rootPaths.hasNext()) || !roots.isEmpty();
  }

  @Override
  public Path next() {
    if (rootPaths == null || !rootPaths.hasNext()) {
      // close as quickly as we can
      if (rootPaths != null) {
        rootPaths.close();
      }
      Node root = roots.remove(0);
      LOG.debug("Traverse a new root taxon: {}", root.getProperty(debugProperty, null));

      rootPaths = td.traverse(root).iterator();
    }
    return rootPaths.next();
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
