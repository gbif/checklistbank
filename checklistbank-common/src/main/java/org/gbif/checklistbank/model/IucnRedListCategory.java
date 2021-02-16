package org.gbif.checklistbank.model;

import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.api.vocabulary.ThreatStatus;

import java.util.Objects;

/**
 * The IUCN RedList Category associated to a species.
 */
public class IucnRedListCategory {

  private ThreatStatus category;

  private Integer usageKey;

  private String scientificName;

  private TaxonomicStatus taxonomicStatus;

  private String acceptedName;

  private Integer acceptedUsageKey;

  public ThreatStatus getCategory() {
    return category;
  }

  public void setCategory(ThreatStatus category) {
    this.category = category;
  }

  public String getCode() {
    return category.getCode();
  }

  public Integer getUsageKey() {
    return usageKey;
  }

  public void setUsageKey(Integer usageKey) {
    this.usageKey = usageKey;
  }

  public String getScientificName() {
    return scientificName;
  }

  public void setScientificName(String scientificName) {
    this.scientificName = scientificName;
  }

  public TaxonomicStatus getTaxonomicStatus() {
    return taxonomicStatus;
  }

  public void setTaxonomicStatus(TaxonomicStatus taxonomicStatus) {
    this.taxonomicStatus = taxonomicStatus;
  }

  public String getAcceptedName() {
    return acceptedName;
  }

  public void setAcceptedName(String acceptedName) {
    this.acceptedName = acceptedName;
  }

  public Integer getAcceptedUsageKey() {
    return acceptedUsageKey;
  }

  public void setAcceptedUsageKey(Integer acceptedUsageKey) {
    this.acceptedUsageKey = acceptedUsageKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IucnRedListCategory that = (IucnRedListCategory) o;
    return category == that.category
           && Objects.equals(usageKey, that.usageKey)
           && Objects.equals(scientificName,
                             that.scientificName)
           && taxonomicStatus == that.taxonomicStatus
           && Objects.equals(acceptedName, that.acceptedName)
           && Objects.equals(acceptedUsageKey, that.acceptedUsageKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(category, usageKey, scientificName, taxonomicStatus, acceptedName, acceptedUsageKey);
  }
}
