package org.gbif.checklistbank.service.mybatis.model;

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
}
