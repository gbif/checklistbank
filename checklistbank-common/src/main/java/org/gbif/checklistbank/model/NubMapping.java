package org.gbif.checklistbank.model;

import org.gbif.api.vocabulary.Origin;

public class NubMapping {
  private Integer usageKey;
  private Integer nubKey;
  private Origin origin;
  private String taxonID;

  public Integer getUsageKey() {
    return usageKey;
  }

  public void setUsageKey(Integer usageKey) {
    this.usageKey = usageKey;
  }

  public Integer getNubKey() {
    return nubKey;
  }

  public void setNubKey(Integer nubKey) {
    this.nubKey = nubKey;
  }

  public Origin getOrigin() {
    return origin;
  }

  public void setOrigin(Origin origin) {
    this.origin = origin;
  }

  public String getTaxonID() {
    return taxonID;
  }

  public void setTaxonID(String taxonID) {
    this.taxonID = taxonID;
  }
}
