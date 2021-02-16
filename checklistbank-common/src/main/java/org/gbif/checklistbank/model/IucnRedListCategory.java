package org.gbif.checklistbank.model;

import org.gbif.api.vocabulary.ThreatStatus;

import java.util.Objects;

/**
 * The IUCN RedList Category associated to a species.
 */
public class IucnRedListCategory {

  private ThreatStatus category;

  private Integer iucnRedListSpeciesKey;

  private String iucnRedListName;

  public ThreatStatus getCategory() {
    return category;
  }

  public void setCategory(ThreatStatus category) {
    this.category = category;
  }

  public String getCode() {
    return category.getCode();
  }

  public Integer getIucnRedListSpeciesKey() {
    return iucnRedListSpeciesKey;
  }

  public void setIucnRedListSpeciesKey(Integer iucnRedListSpeciesKey) {
    this.iucnRedListSpeciesKey = iucnRedListSpeciesKey;
  }

  public String getIucnRedListName() {
    return iucnRedListName;
  }

  public void setIucnRedListName(String iucnRedListName) {
    this.iucnRedListName = iucnRedListName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IucnRedListCategory that = (IucnRedListCategory) o;
    return category == that.category
           && Objects.equals(iucnRedListSpeciesKey, that.iucnRedListSpeciesKey)
           && Objects.equals(iucnRedListName, that.iucnRedListName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(category, iucnRedListSpeciesKey, iucnRedListName);
  }
}
