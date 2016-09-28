package org.gbif.checklistbank.model;

import org.gbif.api.vocabulary.Rank;

import java.util.Objects;

/**
 *
 */
public class NameUsages {
  private Integer[] usageKeys;
  private String scientificName;
  private Rank rank;

  public Integer[] getUsageKeys() {
    return usageKeys;
  }

  public void setUsageKeys(Integer[] usageKeys) {
    this.usageKeys = usageKeys;
  }

  public String getScientificName() {
    return scientificName;
  }

  public void setScientificName(String scientificName) {
    this.scientificName = scientificName;
  }

  public Rank getRank() {
    return rank;
  }

  public void setRank(Rank rank) {
    this.rank = rank;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NameUsages u = (NameUsages) o;
    return usageKeys == u.usageKeys &&
        Objects.equals(scientificName, u.scientificName) &&
        rank == u.rank;
  }

  @Override
  public int hashCode() {
    return Objects.hash(usageKeys, scientificName, rank);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("UsageName{");
    sb.append("keys=").append(usageKeys);
    sb.append(", scientificName=").append(scientificName);
    sb.append(", rank=").append(rank);
    sb.append('}');
    return sb.toString();
  }
}
