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
package org.gbif.checklistbank.utils;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * A boolean matrix with values defaulting to false.
 * The matrix is an indentiy matrix and symmetric.
 */
public class SymmetricIdentityMatrix<T> {
  private Map<T, Set<T>> matches = Maps.newHashMap();

  private SymmetricIdentityMatrix() {

  }

  public static <T> SymmetricIdentityMatrix<T> create() {
    return new SymmetricIdentityMatrix<T>();
  }

  public void add(T val1, T val2) {
    if (!matches.containsKey(val1)) {
      matches.put(val1, Sets.newHashSet());
    }
    matches.get(val1).add(val2);
  }

  public void remove(T val) {
    matches.remove(val);
    for (Set<T> vals : matches.values()) {
      vals.remove(val);
    }
  }

  /**
   * Check if 2 values in the matrix exist or are identical.
   * @param val1
   * @param val2
   * @return true if values are identical or have an entry in the symmetric matrix
   */
  public boolean contains(T val1, T val2) {
    return val1.equals(val2)
            || matches.containsKey(val1) && matches.get(val1).contains(val2)
            || matches.containsKey(val2) && matches.get(val2).contains(val1);
  }
}
