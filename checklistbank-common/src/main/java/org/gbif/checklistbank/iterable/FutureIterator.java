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
package org.gbif.checklistbank.iterable;

import java.util.Iterator;
import java.util.concurrent.Future;

/**
 * A simple wrapper for a list of futures providing an iterator for their results.
 * This allows to process futures in the order as they were originally submitted.
 */
public class FutureIterator<T> implements Iterator<T> {
  private final Iterator<Future<T>> iter;

  public FutureIterator(Iterable<Future<T>> futures) {
    iter = futures.iterator();
  }

  @Override
  public boolean hasNext() {
    return iter.hasNext();
  }

  @Override
  public T next() {
    Future<T> f = iter.next();
    try {
      return f.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void remove() {
    iter.remove();
  }
}
