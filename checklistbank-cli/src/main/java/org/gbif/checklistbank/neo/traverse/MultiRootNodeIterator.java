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

import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.traversal.TraversalDescription;

import com.google.common.collect.Lists;

/**
 * Path iterator that traverses multiple start nodes in a given traversal description.
 */
public class MultiRootNodeIterator extends MultiRooIterator<Node> {

  private final TraversalDescription td;

  private MultiRootNodeIterator(List<Node> roots, TraversalDescription td) {
    super(roots);
    this.td = td;
    prefetch();
  }

  public static ResourceIterable<Node> create(final Node root, final TraversalDescription td) {
    return create(Lists.newArrayList(root), td);
  }

  public static ResourceIterable<Node> create(final List<Node> roots, final TraversalDescription td) {
    return new ResourceIterable<Node>() {
      @Override
      public ResourceIterator<Node> iterator() {
        return new MultiRootNodeIterator(roots, td);
      }
    };
  }

  @Override
  ResourceIterator<Node> iterateRoot(Node root) {
    return td.traverse(root).nodes().iterator();
  }

}
