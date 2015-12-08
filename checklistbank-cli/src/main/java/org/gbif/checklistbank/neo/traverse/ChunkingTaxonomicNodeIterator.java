package org.gbif.checklistbank.neo.traverse;

import org.gbif.dwc.terms.DwcTerm;

import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.ResourceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node iterator that traverses all nodes in a taxonomic parent-child order starting from the top root nodes.
 * It stops when the tree is sufficiently broad, see {@link Traversals#ACCEPTED_TREE_AND_MARK_CHUNKS}.
 */
public class ChunkingTaxonomicNodeIterator extends TaxonomicNodeIterator {

  private static final Logger LOG = LoggerFactory.getLogger(ChunkingTaxonomicNodeIterator.class);

  private ChunkingTaxonomicNodeIterator(List<Node> roots) {
    super(roots, true);
  }

  @Override
  ResourceIterator<Path> getDescendants(Node root) {
    LOG.debug("Chunk-traverse a new root taxon: {}", root.getProperty(DwcTerm.scientificName.simpleName(), null));
    return Traversals.ACCEPTED_TREE_AND_MARK_CHUNKS.traverse(root).iterator();
  }
}
