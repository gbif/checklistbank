package org.gbif.checklistbank.model;

public class Citation {
  private Integer key;
  private String citation;

  public Citation() {
  }

  public Citation(String citation) {
    this.citation = citation;
  }

  public Integer getKey() {
    return key;
  }

  public void setKey(Integer key) {
    this.key = key;
  }

  public String getCitation() {
    return citation;
  }

  public void setCitation(String citation) {
    this.citation = citation;
  }
}
