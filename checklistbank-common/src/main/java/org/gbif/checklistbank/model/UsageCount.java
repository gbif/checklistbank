package org.gbif.checklistbank.model;

import org.gbif.api.vocabulary.Rank;

/**
 * simple usage class for displaying tree maps that need a size attribute with a label and key
 */
public class UsageCount {
  private int key;
  private String name;
  private Rank rank;
  private int size;

  public int getKey() {
    return key;
  }

  public void setKey(int key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Rank getRank() {
    return rank;
  }

  public void setRank(Rank rank) {
    this.rank = rank;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }
}
