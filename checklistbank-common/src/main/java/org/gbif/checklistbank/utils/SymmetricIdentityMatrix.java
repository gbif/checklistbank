package org.gbif.checklistbank.utils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

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
