package org.gbif.checklistbank.model;

import org.gbif.api.vocabulary.Rank;

import java.util.Objects;

/**
 *
 */
public class ScientificName {
  private int key;
  private String scientificName;
  private Rank rank;

  public int getKey() {
    return key;
  }

  public void setKey(int key) {
    this.key = key;
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
    ScientificName u = (ScientificName) o;
    return key == u.key &&
        Objects.equals(scientificName, u.scientificName) &&
        rank == u.rank;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, scientificName, rank);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ScientificName{");
    sb.append("key=").append(key);
    sb.append(", scientificName=").append(scientificName);
    sb.append(", rank=").append(rank);
    sb.append('}');
    return sb.toString();
  }
}
