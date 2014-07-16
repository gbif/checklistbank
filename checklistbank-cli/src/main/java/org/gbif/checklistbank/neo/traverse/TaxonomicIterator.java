package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.Labels;
import org.gbif.dwc.terms.DwcTerm;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Path iterator that traverses all nodes in a taxonomic parent-child order starting from the top root nodes to the
 * lowest taxon.
 */
public class TaxonomicIterator implements AutoCloseable, Iterator<Path> {
    private static final Logger LOG = LoggerFactory.getLogger(TaxonomicIterator.class);

    private final ResourceIterator<Node> roots;
    private ResourceIterator<Path> descendants;
    private TraversalDescription td;

    private TaxonomicIterator(ResourceIterator<Node> roots, TraversalDescription td) {
        this.td = td;
        //TODO: sort root nodes by rank & sciName
        this.roots = roots;
    }

    public static Iterable<Path> all(final GraphDatabaseService db) {
        return new Iterable<Path>() {
            @Override
            public Iterator<Path> iterator() {
                return new TaxonomicIterator(GlobalGraphOperations.at(db).getAllNodesWithLabel(Labels.ROOT).iterator(),
                    db.traversalDescription().depthFirst().expand(new TaxonomicOrderExpander()));
            }
        };
    }

    @Override
    public boolean hasNext() {
        return (descendants != null && descendants.hasNext()) || roots.hasNext();
    }

    @Override
    public Path next() {
        if (descendants == null || !descendants.hasNext()) {
            // close as quickly as we can
            if (descendants != null) {
                descendants.close();
            }
            Node root = roots.next();
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
        roots.close();
    }

}
