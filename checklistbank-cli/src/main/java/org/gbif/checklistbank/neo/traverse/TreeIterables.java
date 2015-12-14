package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.vocabulary.Rank;

import javax.annotation.Nullable;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

/**
 * Utils to create Iterables for nodes to traverse a taxonomic tree in taxonomic order but with unsorted leaf nodes.
 */
public class TreeIterables {

  public static Iterable<Node> allNodes(GraphDatabaseService db, @Nullable Node root, @Nullable Rank lowestRank, boolean inclProParte) {
    return MultiRootNodeIterator.create(TreeIterablesSorted.findRoot(db, root), TreeIterablesSorted.filterRank(inclProParte ? Traversals.TREE : Traversals.SORTED_TREE_WITHOUT_PRO_PARTE, lowestRank));
  }

}
