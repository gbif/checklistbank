package org.gbif.checklistbank.neo.traverse;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.yammer.metrics.Meter;
import org.gbif.dwc.terms.DwcTerm;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 *
 */
public class TaxonWalker {
    private static final Logger LOG = LoggerFactory.getLogger(TaxonWalker.class);

    public static void walkAll(GraphDatabaseService db, StartEndHandler handler) {
        walkAll(db, handler, 10000, null);
    }

    /**
     * Walks all nodes in a transaction that is renewed in a given batchsize
     * @param db
     * @param handler
     * @param batchsize number of paths that should be walked within a single open transaction
     */
    public static void walkAll(GraphDatabaseService db, StartEndHandler handler, int batchsize, @Nullable Meter meter) {
        Path lastPath = null;
        long counter = 0;
        Transaction tx = db.beginTx();
        try {
            for (Path p : TaxonomicIterator.all(db)) {
                if (batchsize > 0 && counter % batchsize == 0) {
                    tx.success();
                    tx.close();
                    LOG.debug("Opening new transaction, record {}", counter);
                    if (meter != null) {
                        LOG.debug("Processing rate = {}", meter.getMeanRate());
                    }
                    tx = db.beginTx();
                }
                if (meter != null) {
                    meter.mark();
                }
                //logPath(p);
                if (lastPath != null) {
                    PeekingIterator<Node> lIter = Iterators.peekingIterator(lastPath.nodes().iterator());
                    PeekingIterator<Node> cIter = Iterators.peekingIterator(p.nodes().iterator());
                    while (lIter.hasNext() && cIter.hasNext() && lIter.peek().equals(cIter.peek())) {
                        lIter.next();
                        cIter.next();
                    }
                    // only non shared nodes left.
                    // first close all old nodes, then open new ones
                    // reverse order for closing nodes...
                    for (Node n : ImmutableList.copyOf(lIter).reverse()) {
                        handler.end(n);
                    }
                    while (cIter.hasNext()) {
                        handler.start(cIter.next());
                    }

                } else {
                    // only new nodes
                    for (Node n : p.nodes()) {
                        handler.start(n);
                    }
                }
                lastPath = p;
                counter++;
            }
            tx.success();

        } finally {
            tx.close();
        }
    }

    private static void logPath(Path p) {
        StringBuilder sb = new StringBuilder();
        for (Node n : p.nodes()) {
            if (sb.length() > 0) {
                sb.append(" -- ");
            }
            sb.append((String) n.getProperty(DwcTerm.scientificName.simpleName()));
        }
        sb.append(", " + p.endNode().getProperty(DwcTerm.taxonRank.simpleName()));
        LOG.debug(sb.toString());
    }

}
