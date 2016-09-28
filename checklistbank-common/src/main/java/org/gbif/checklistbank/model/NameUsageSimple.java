package org.gbif.checklistbank.model;

import org.gbif.api.vocabulary.Rank;

import java.util.Objects;

/**
 *
 */
public class NameUsageSimple {
  private int usageKey;
  private String scientificName;
  private Rank rank;

  public int getUsageKey() {
    return usageKey;
  }

  public void setUsageKey(int usageKey) {
    this.usageKey = usageKey;
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
    NameUsageSimple u = (NameUsageSimple) o;
    return usageKey == u.usageKey &&
        Objects.equals(scientificName, u.scientificName) &&
        rank == u.rank;
  }

  @Override
  public int hashCode() {
    return Objects.hash(usageKey, scientificName, rank);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("UsageName{");
    sb.append("key=").append(usageKey);
    sb.append(", scientificName=").append(scientificName);
    sb.append(", rank=").append(rank);
    sb.append('}');
    return sb.toString();
  }
}
