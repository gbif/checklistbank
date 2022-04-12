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
package org.gbif.checklistbank.service;

import org.gbif.api.model.checklistbank.NameUsage;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Persistence service dealing with name usages.
 * This interface is restricted to the mybatis module only!
 */
public interface UsageService {

  /**
   * @return int array of all current (not deleted) name usage ids in checklist bank
   */
  List<Integer> listAll();

  /**
   * @return the highest usageKey used in the dataset
   */
  Integer maxUsageKey(UUID datasetKey);

  /**
   * @return the highest usageKey used in the dataset
   */
  Integer minUsageKey(UUID datasetKey);

  /**
   * Lists all name usages with a key between start / end.
   */
  List<NameUsage> listRange(int usageKeyStart, int usageKeyEnd);

  /**
   * Lists classification as parent keys.
   */
  List<Integer> listParents(int usageKey);

  /**
   * Lists all old name usage ids last interpreted before the given date.
   */
  List<Integer> listOldUsages(UUID datasetKey, Date before);

}
