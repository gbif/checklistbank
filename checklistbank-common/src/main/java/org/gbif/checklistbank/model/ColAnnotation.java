package org.gbif.checklistbank.model;

import java.util.Objects;

public class ColAnnotation {
  private Integer taxonId;
  private String gsd;
  private String annotatedName;
  private boolean rejected;
  private String status;
  private String note;

  public ColAnnotation() {
  }

  public ColAnnotation(Integer taxonId, String gsd, String annotatedName, boolean rejected, String status, String note) {
    this.taxonId = taxonId;
    this.gsd = gsd;
    this.annotatedName = annotatedName;
    this.rejected = rejected;
    this.status = status;
    this.note = note;
  }

  public Integer getTaxonId() {
    return taxonId;
  }

  public void setTaxonId(Integer taxonId) {
    this.taxonId = taxonId;
  }

  public String getGsd() {
    return gsd;
  }

  public void setGsd(String gsd) {
    this.gsd = gsd;
  }

  public boolean isRejected() {
    return rejected;
  }

  public void setRejected(boolean rejected) {
    this.rejected = rejected;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getAnnotatedName() {
    return annotatedName;
  }

  public void setAnnotatedName(String annotatedName) {
    this.annotatedName = annotatedName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(taxonId, gsd, annotatedName, rejected, status, note);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ColAnnotation other = (ColAnnotation) obj;
    return Objects.equals(this.taxonId, other.taxonId) && Objects.equals(this.gsd, other.gsd) && Objects
      .equals(this.annotatedName, other.annotatedName) && Objects.equals(this.rejected, other.rejected) && Objects
             .equals(this.status, other.status) && Objects.equals(this.note, other.note);
  }
}
