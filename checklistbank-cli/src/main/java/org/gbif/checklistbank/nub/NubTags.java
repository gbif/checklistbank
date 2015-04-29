package org.gbif.checklistbank.nub;

public enum NubTags {
  PRIORITY("priority"),
  RANK_LIMIT("rankLimit");

  public static final String NAMESPACE = "nub.gbif.org";
  public final String tag;

  private NubTags(String tag) {
    this.tag = tag;
  }
}
