package org.gbif.checklistbank.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Maps;

/**
 *
 */
public class TreeContainer<T, KEY> {
  private List<T> root;
  private Map<KEY, List<T>> children = Maps.newHashMap();

  public Map<KEY, List<T>> getChildren() {
    return children;
  }

  public void setChildren(Map<KEY, List<T>> children) {
    this.children = children;
  }

  public List<T> getRoot() {
    return root;
  }

  public void setRoot(List<T> root) {
    this.root = root;
  }

  @Override
  public int hashCode() {
    return Objects.hash(root, children);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final TreeContainer other = (TreeContainer) obj;
    return Objects.equals(this.root, other.root)
        && Objects.equals(this.children, other.children);
  }
}
