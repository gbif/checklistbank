package org.gbif.checklistbank.model;

import java.util.Objects;

/**
 * Internal class used by mybatis layer to retrieve an extension instance together with the usage key it belongs to.
 */
public class UsageRelated<T> {
  private int usageKey;
  private T value;

  public int getUsageKey() {
    return usageKey;
  }

  public void setUsageKey(int usageKey) {
    this.usageKey = usageKey;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(usageKey, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final UsageRelated other = (UsageRelated) obj;
    return Objects.equals(this.usageKey, other.usageKey) && Objects.equals(this.value, other.value);
  }
}
