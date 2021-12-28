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

import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.util.Objects;

/**
 * A simple usage class for nub building only with minimal footprint to save memory.
 */
public class Usage {
  public int key;
  public int nameKey;
  public int parentKey;
  public int namePublishedInKey;
  public TaxonomicStatus status;
  public Rank rank;
  public Origin origin;
  public int sourceKey;
  public int accordingToKey;

  public boolean hasParent() {
    return parentKey > 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, nameKey, parentKey, namePublishedInKey, status, rank, origin, sourceKey, accordingToKey);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Usage other = (Usage) obj;
    return Objects.equals(this.key, other.key) && Objects.equals(this.nameKey, other.nameKey) && Objects
      .equals(this.parentKey, other.parentKey) && Objects.equals(this.namePublishedInKey, other.namePublishedInKey)
           && Objects.equals(this.status, other.status) && Objects.equals(this.rank, other.rank) && Objects
      .equals(this.origin, other.origin) && Objects.equals(this.sourceKey, other.sourceKey) && Objects
             .equals(this.accordingToKey, other.accordingToKey);
  }
}
