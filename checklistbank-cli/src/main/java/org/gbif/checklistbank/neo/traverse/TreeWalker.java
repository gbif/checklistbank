package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.NeoProperties;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.yammer.metrics.Meter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class to iterate over nodes in taxonomic order and execute any number of StartEndHandler while walking.
 */
public class TreeWalker {

  private static final Logger LOG = LoggerFactory.getLogger(TreeWalker.class);
  private static final int reportingSize = 10000;

  public static void walkTree(GraphDatabaseService db, boolean inclProParte, StartEndHandler ... handler) {
    walkTree(db, inclProParte, null, null, null, handler);
  }

  /**
   * Walks all nodes in a taxonomic tree order in a single transaction including pro parte relations
   * @param root if given starts to walk the subtree including the given node
   */
  public static void walkTree(GraphDatabaseService db, boolean inclProParte, @Nullable Node root, @Nullable Rank lowestRank, @Nullable Meter meter, StartEndHandler ... handler) {
    try (Transaction tx = db.beginTx()){
      walkTree(TreeIterablesSorted.allPath(db, root, lowestRank, inclProParte), meter, handler);
    }
  }

  /**
   * Walks allAccepted nodes in a single transaction
   */
  public static void walkAcceptedTree(GraphDatabaseService db, StartEndHandler ... handler) {
    walkAcceptedTree(db, null, null, new Meter(), handler);
  }

  /**
   * Walks allAccepted nodes in a single transaction
   */
  public static void walkAcceptedTree(GraphDatabaseService db, @Nullable Node root, @Nullable Rank lowestRank, @Nullable Meter meter, StartEndHandler ... handler) {
    try (Transaction tx = db.beginTx()){
      walkTree(TreeIterablesSorted.acceptedPath(db, root, lowestRank), meter, handler);
    }
  }


  private static void walkTree(ResourceIterable<Path> paths, @Nullable Meter meter, StartEndHandler ... handler) {
    Path lastPath = null;
    long counter = 0;
    try (ResourceIterator<Path> iter = paths.iterator()){
      while (iter.hasNext()) {
        Path p = iter.next();
        //logPath(p);
        if (counter % reportingSize == 0) {
          LOG.debug("Processed {}. Rate = {}", counter, meter == null ? "unknown" : meter.getMeanRate());
        }
        if (meter != null) {
          meter.mark();
        }
        if (lastPath != null) {
          PeekingIterator<Node> lIter = Iterators.peekingIterator(lastPath.nodes().iterator());
          PeekingIterator<Node> cIter = Iterators.peekingIterator(p.nodes().iterator());
          while (lIter.hasNext() && cIter.hasNext() && lIter.peek().equals(cIter.peek())) {
            lIter.next();
            cIter.next();
          }
          // only non shared nodes left.
          // first close allAccepted old nodes, then open new ones
          // reverse order for closing nodes...
          for (Node n : ImmutableList.copyOf(lIter).reverse()) {
            handleEnd(n, handler);
          }
          while (cIter.hasNext()) {
            handleStart(cIter.next(), handler);
          }

        } else {
          // only new nodes
          for (Node n : p.nodes()) {
            handleStart(n, handler);
          }
        }
        lastPath = p;
        counter++;
      }
      // close all remaining nodes
      if (lastPath != null) {
        for (Node n : ImmutableList.copyOf(lastPath.nodes()).reverse()) {
          handleEnd(n, handler);
        }
      }
    }
  }

  private static void handleStart(Node n, StartEndHandler ... handler) {
    for (StartEndHandler h : handler) {
      h.start(n);
    }
  }

  private static void handleEnd(Node n, StartEndHandler ... handler) {
    for (StartEndHandler h : handler) {
      h.end(n);
    }
  }

  private static void logPath(Path p) {
    StringBuilder sb = new StringBuilder();
    for (Node n : p.nodes()) {
      if (sb.length() > 0) {
        sb.append(" -- ");
      }
      sb.append(NeoProperties.getCanonicalName(n));
    }
    LOG.info(sb.toString());
  }

}
