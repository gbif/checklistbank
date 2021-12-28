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

/**
 *
 */
public class UsageForeignKeys {
  private int usageKey;
  private Integer parentKey;
  private Integer basionymKey;

  public UsageForeignKeys() {
  }

  public UsageForeignKeys(int usageKey) {
    this.usageKey = usageKey;
  }

  public UsageForeignKeys(int usageKey, Integer parentKey, Integer basionymKey) {
    this.basionymKey = basionymKey;
    this.parentKey = parentKey;
    this.usageKey = usageKey;
  }

  public Integer getBasionymKey() {
    return basionymKey;
  }

  public void setBasionymKey(Integer basionymKey) {
    this.basionymKey = basionymKey;
  }

  public Integer getParentKey() {
    return parentKey;
  }

  public void setParentKey(Integer parentKey) {
    this.parentKey = parentKey;
  }

  public int getUsageKey() {
    return usageKey;
  }

  public void setUsageKey(int usageKey) {
    this.usageKey = usageKey;
  }
}
