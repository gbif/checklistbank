/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    return category != null? category.getCode() : null;
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
