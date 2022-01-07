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

import org.gbif.checklistbank.neo.NeoProperties;

import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Ordering;

/**
 * Path iterator that traverses multiple start nodes in a given traversal description.
 */
public abstract class MultiRooIterator<T> implements AutoCloseable, ResourceIterator<T> {

  private static final Logger LOG = LoggerFactory.getLogger(MultiRooIterator.class);

  private final Iterator<Node> roots;
  private ResourceIterator<T> rootPaths;
  private T next;

  MultiRooIterator(List<Node> roots) {
    this.roots = Ordering.from(new TaxonomicOrder()).sortedCopy(roots).iterator();
    LOG.debug("New iterator with {} root nodes created", roots.size());
  }

  @Override
  public boolean hasNext() {
    return next != null;
  }

  @Override
  public T next() {
    T p = next;
    prefetch();
    return p;
  }

  abstract ResourceIterator<T> iterateRoot(Node root);

  public void prefetch() {
    while ((rootPaths == null || !rootPaths.hasNext()) && roots.hasNext()) {
      // close as quickly as we can
      if (rootPaths != null) {
        rootPaths.close();
      }
      Node root = roots.next();
      LOG.debug("Traverse a new root taxon: {}", NeoProperties.getCanonicalName(root));
      rootPaths = iterateRoot(root);
    }
    if (rootPaths != null && rootPaths.hasNext()) {
      next = rootPaths.next();
    } else {
      next = null;
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    if (rootPaths != null) {
      rootPaths.close();
    }
  }

}
