package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.dwc.terms.DwcTerm;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node iterator that traverses all or just accepted nodes in a taxonomic parent-child order starting from the top root nodes to
 * the lowest taxon.
 */
public class TaxonomicNodeIterator implements AutoCloseable, Iterator<Node> {

  private static final Logger LOG = LoggerFactory.getLogger(TaxonomicNodeIterator.class);

  private final List<Node> roots;
  private ResourceIterator<Path> descendants;
  private Iterator<Relationship> synonyms;
  private final boolean inclSynonyms;

  TaxonomicNodeIterator(List<Node> roots, boolean synonyms) {
    Collections.sort(roots, new TaxonOrder());
    this.roots = roots;
    this.inclSynonyms = synonyms;
  }

  public static Iterable<Node> accepted(final GraphDatabaseService db) {
    return new Iterable<Node>() {
      @Override
      public Iterator<Node> iterator() {
        return new TaxonomicNodeIterator(IteratorUtil.asList(db.findNodes(Labels.ROOT)), false);
      }
    };
  }

  /**
   * Iterates over all taxa from root down, also returning all synonyms for each accepted taxon.
   */
  public static Iterable<Node> all(final GraphDatabaseService db) {
    return new Iterable<Node>() {
      @Override
      public Iterator<Node> iterator() {
        return new TaxonomicNodeIterator(IteratorUtil.asList(db.findNodes(Labels.ROOT)), true);
      }
    };
  }

  /**
   * Iterates over all taxa from the given node down,, also returning all synonyms for each accepted taxon.
   */
  public static Iterable<Node> all(final GraphDatabaseService db, final long id) {
    return new Iterable<Node>() {
      @Override
      public Iterator<Node> iterator() {
        return new TaxonomicNodeIterator(Lists.newArrayList(db.getNodeById(id)), true);
      }
    };
  }

  @Override
  public boolean hasNext() {
    return (descendants != null && descendants.hasNext()) || !roots.isEmpty() || (synonyms != null
                                                                                  && synonyms.hasNext());
  }

  @Override
  public Node next() {
    // first iterate over potential synonyms
    if (synonyms != null && synonyms.hasNext()) {
      return synonyms.next().getStartNode();
    }
    // check if a current descendants iterator exists with more records, create a new one otherwise
    if (descendants == null || !descendants.hasNext()) {
      // close old one
      if (descendants != null) {
        descendants.close();
      }
      Node root = roots.remove(0);
      descendants = getDescendants(root);
    }
    Path descendant = descendants.next();
    if (inclSynonyms && descendant != null) {
      //readUsage synonym list
      synonyms = descendant.endNode().getRelationships(RelType.SYNONYM_OF, Direction.INCOMING).iterator();
    }
    return descendant.endNode();
  }

  ResourceIterator<Path> getDescendants(Node root) {
    LOG.debug("Traverse a new root taxon: {}", root.getProperty(DwcTerm.scientificName.simpleName(), null));
    return Traversals.ACCEPTED_TREE.traverse(root).iterator();
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
