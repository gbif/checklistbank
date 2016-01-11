package org.gbif.checklistbank.model;

/**
 *
 */
public class UsageForeignKeys {
  private int usageKey;
  private Integer parentKey;
  private Integer basionymKey;

  public UsageForeignKeys(int usageKey, Integer parentKey, Integer basionymKey) {
    this.basionymKey = basionymKey;
    this.parentKey = parentKey;
    this.usageKey = usageKey;
  }

  public Integer getBasionymKey() {
    return basionymKey;
  }

  public void setBasionymKey(Integer basionymKey) {
    this.basionymKey = basionymKey;
  }

  public Integer getParentKey() {
    return parentKey;
  }

  public void setParentKey(Integer parentKey) {
    this.parentKey = parentKey;
  }

  public int getUsageKey() {
    return usageKey;
  }

  public void setUsageKey(int usageKey) {
    this.usageKey = usageKey;
  }
}
