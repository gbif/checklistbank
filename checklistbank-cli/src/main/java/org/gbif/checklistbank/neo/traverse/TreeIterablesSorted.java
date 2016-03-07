package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.Labels;

import java.util.List;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;

/**
 * Utils to persistent Iterables for nodes or paths to traverse a taxonomic tree in taxonomic order with sorted leaf nodes.
 */
public class TreeIterablesSorted {

  /**
   * Iterates over all nodes in taxonomic hierarchy, but unsorted withing each branch.
   */
  public static ResourceIterable<Node> allNodes(GraphDatabaseService db, @Nullable Node root, @Nullable Rank lowestRank, boolean inclProParte) {
    return MultiRootNodeIterator.create(findRoot(db, root), filterRank(inclProParte ? Traversals.SORTED_TREE : Traversals.SORTED_TREE_WITHOUT_PRO_PARTE, lowestRank));
  }

  /**
   * Iterates over all paths
   */
  public static ResourceIterable<Path> allPath(GraphDatabaseService db, @Nullable Node root, @Nullable Rank lowestRank, boolean inclProParte) {
    return MultiRootPathIterator.create(findRoot(db, root), filterRank(inclProParte ? Traversals.SORTED_TREE : Traversals.SORTED_TREE_WITHOUT_PRO_PARTE, lowestRank));
  }

  /**
   * Iterates over all paths ending in an accepted node.
   */
  public static ResourceIterable<Path> acceptedPath(GraphDatabaseService db, @Nullable Node root, @Nullable Rank lowestRank) {
    return MultiRootPathIterator.create(findRoot(db, root), filterRank(Traversals.SORTED_ACCEPTED_TREE, lowestRank));
  }


  public static List<Node> findRoot(GraphDatabaseService db) {
    return findRoot(db, null);
  }

  protected static List<Node> findRoot(GraphDatabaseService db, @Nullable Node root) {
    if (root != null) {
      return Lists.newArrayList(root);
    }
    return IteratorUtil.asList(db.findNodes(Labels.ROOT));
  }

  public static TraversalDescription filterRank(TraversalDescription td, @Nullable Rank lowestRank) {
    if (lowestRank != null) {
      return td.evaluator(new RankEvaluator(lowestRank));
    }
    return td;
  }
}
