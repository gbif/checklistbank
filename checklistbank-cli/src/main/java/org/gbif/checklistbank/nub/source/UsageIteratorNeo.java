package org.gbif.checklistbank.nub.source;

import org.gbif.checklistbank.iterable.CloseableIterable;
import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.io.IOException;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A neo db backed iterable that can be used to iterate over all usages in the source multiple times.
 * The iteration is in taxonomic order, starting with the highest root taxa and walks
 * the taxonomic tree in depth order first, including synonyms.
 */
public class UsageIteratorNeo implements CloseableIterable<SrcUsage> {
    private static final Logger LOG = LoggerFactory.getLogger(UsageIteratorNeo.class);
    private UsageDao dao;

    public UsageIteratorNeo(UsageDao dao) {
        this.dao = dao;
    }

    /**
     * Closes dao and deletes all intermediate persistence files.
     */
    @Override
    public void close() throws IOException {
        dao.closeAndDelete();
    }

    public class SrcUsageIterator implements CloseableIterator<SrcUsage> {
        private final Transaction tx;
        private final ResourceIterator<Node> nodes;
        private final Node root;

        public SrcUsageIterator(UsageDao dao) {
            tx = dao.beginTx();
            root = IteratorUtil.first(dao.getNeo().findNodes(Labels.ROOT));
            this.nodes = Traversals.DESCENDANTS.traverse(root).nodes().iterator();
        }

        @Override
        public boolean hasNext() {
            return nodes.hasNext();
        }

        @Override
        public SrcUsage next() {
            return dao.readSourceUsage(nodes.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public void close() {
            nodes.close();
            tx.close();
        }
    }

    @Override
    public CloseableIterator<SrcUsage> iterator() {
        return new SrcUsageIterator(dao);
    }


}
