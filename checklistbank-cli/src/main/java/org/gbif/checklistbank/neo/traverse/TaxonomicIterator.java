package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.Labels;
import org.gbif.dwc.terms.DwcTerm;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Path iterator that traverses allAccepted nodes in a taxonomic parent-child order starting from the top root nodes to
 * the
 * lowest taxon.
 */
public class TaxonomicIterator implements AutoCloseable, Iterator<Path> {

  private static final Logger LOG = LoggerFactory.getLogger(TaxonomicIterator.class);

  private final List<Node> roots;
  private ResourceIterator<Path> descendants;
  private TraversalDescription td;

  private TaxonomicIterator(List<Node> roots, TraversalDescription td) {
    this.td = td;
    Collections.sort(roots, new TaxonOrder());
    this.roots = roots;
  }

  public static Iterable<Path> allAccepted(final GraphDatabaseService db) {
    return new Iterable<Path>() {
      @Override
      public Iterator<Path> iterator() {
        List<Node> roots = IteratorUtil.asList(db.findNodes(Labels.ROOT));
        return new TaxonomicIterator(roots, db.traversalDescription()
                                      .depthFirst()
                                      .expand(new TaxonomicOrderExpander())
                                      .evaluator(new AcceptedOnlyEvaluator())
        );
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
      LOG.debug("Traverse a new root taxon: {}", root.getProperty(DwcTerm.scientificName.simpleName(), null));
      descendants = td.traverse(root).iterator();
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
