package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Path iterator that traverses allAccepted nodes in a taxonomic parent-child order starting from the top root nodes to
 * the
 * lowest taxon.
 */
public class TaxonomicPathIterator implements AutoCloseable, Iterator<Path> {

  private static final Logger LOG = LoggerFactory.getLogger(TaxonomicPathIterator.class);

  private final List<Node> roots;
  private ResourceIterator<Path> descendants;

  private TaxonomicPathIterator(List<Node> roots) {
    Collections.sort(roots, new TaxonOrder());
    this.roots = roots;
    LOG.debug("Found {} root nodes to iterate over", roots.size());
  }

  public static Iterable<Path> allAccepted(final GraphDatabaseService db) {
    return new Iterable<Path>() {
      @Override
      public Iterator<Path> iterator() {
        return new TaxonomicPathIterator(IteratorUtil.asList(db.findNodes(Labels.ROOT)));
      }
    };
  }

  @Override
  public boolean hasNext() {
    return (descendants != null && descendants.hasNext()) || !roots.isEmpty();
  }

  @Override
  public Path next() {
    if (descendants == null || !descendants.hasNext()) {
      // close as quickly as we can
      if (descendants != null) {
        descendants.close();
      }
      Node root = roots.remove(0);
      LOG.debug("Traverse a new root taxon: {}", root.getProperty(NeoProperties.SCIENTIFIC_NAME, null));
      descendants = Traversals.ACCEPTED_DESCENDANTS.traverse(root).iterator();
    }
    return descendants.next();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() throws Exception {
    if (descendants != null) {
      descendants.close();
    }
  }

}
