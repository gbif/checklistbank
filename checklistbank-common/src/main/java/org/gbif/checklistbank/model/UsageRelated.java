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

import java.util.Objects;

/**
 * Internal class used by mybatis layer to retrieve an extension instance together with the usage key it belongs to.
 */
public class UsageRelated<T> {
  private int usageKey;
  private T value;

  public int getUsageKey() {
    return usageKey;
  }

  public void setUsageKey(int usageKey) {
    this.usageKey = usageKey;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(usageKey, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final UsageRelated other = (UsageRelated) obj;
    return Objects.equals(this.usageKey, other.usageKey) && Objects.equals(this.value, other.value);
  }
}
