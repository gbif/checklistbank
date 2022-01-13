/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.Labels;

import java.util.List;

import javax.annotation.Nullable;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.Iterators;

import com.google.common.collect.Lists;


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
    return Iterators.asList(db.findNodes(Labels.ROOT));
  }

  public static TraversalDescription filterRank(TraversalDescription td, @Nullable Rank lowestRank) {
    if (lowestRank != null) {
      return td.evaluator(new RankEvaluator(lowestRank));
    }
    return td;
  }
}
