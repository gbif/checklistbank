package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.dwc.terms.DwcTerm;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Path iterator that traverses allAccepted nodes in a taxonomic parent-child order starting from the top root nodes to the
 * lowest taxon.
 */
public class TaxonomicNodeIterator implements AutoCloseable, Iterator<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(TaxonomicNodeIterator.class);

    private final List<Node> roots;
    private ResourceIterator<Path> descendants;
    private Iterator<Relationship> synonyms;
    private TraversalDescription td;
    private final boolean inclSynonyms;

    private TaxonomicNodeIterator(List<Node> roots, TraversalDescription td, boolean synonyms) {
        this.td = td;
        Collections.sort(roots, new TaxonOrder());
        this.roots = roots;
        this.inclSynonyms = synonyms;
    }

    public static Iterable<Node> allAccepted(final GraphDatabaseService db) {
        return new Iterable<Node>() {
            @Override
            public Iterator<Node> iterator() {
                List<Node> roots = IteratorUtil.asList(GlobalGraphOperations.at(db).getAllNodesWithLabel(Labels.ROOT));
                return new TaxonomicNodeIterator(roots, db.traversalDescription().depthFirst().expand(new TaxonomicOrderExpander()), false);
            }
        };
    }

    /**
     * Iterates over all taxa from root down, also returning all synonyms for each accepted taxon.
     * TODO: how to handle basionyms ???????
     */
    public static Iterable<Node> all(final GraphDatabaseService db) {
        return new Iterable<Node>() {
            @Override
            public Iterator<Node> iterator() {
                List<Node> roots = IteratorUtil.asList(GlobalGraphOperations.at(db).getAllNodesWithLabel(Labels.ROOT));
                return new TaxonomicNodeIterator(roots, db.traversalDescription().depthFirst().expand(new TaxonomicOrderExpander()), true);
            }
        };
    }

    @Override
    public boolean hasNext() {
        return (descendants != null && descendants.hasNext()) || !roots.isEmpty() || (synonyms != null && synonyms.hasNext());
    }

    @Override
    public Node next() {
        if (synonyms != null && synonyms.hasNext()) {
            return synonyms.next().getStartNode();
        }
        if (descendants == null || !descendants.hasNext()) {
            // close as quickly as we can
            if (descendants != null) {
                descendants.close();
            }
            Node root = roots.remove(0);
            LOG.debug("Traverse a new root taxon: {}", root.getProperty(DwcTerm.scientificName.simpleName(), null));
            descendants = td.traverse(root).iterator();
        }
        Path descendant = descendants.next();
        if (inclSynonyms && descendant != null) {
            //read synonym list
            synonyms = descendant.endNode().getRelationships(RelType.SYNONYM_OF, Direction.INCOMING).iterator();
        }
        return descendant.endNode();
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
