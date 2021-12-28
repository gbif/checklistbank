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
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.checklistbank.model.UsageForeignKeys;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Persistence service dealing with methods needed to import new checklists into checklistbank.
 * The methods are mostly doing batch operations and larger operations, hardly any single record modifications.
 * This interface is restricted to the mybatis module only!
 */
public interface DatasetImportService extends AutoCloseable {

  /**
   * @param usageNeoIds neo4j node ids as ints
   * @return list of clb usage keys that were synced
   */
  Future<List<Integer>> sync(UUID datasetKey, ImporterCallback dao, Iterable<Integer> usageNeoIds);

  /**
   * @param datasetKey
   * @param usages list of usages
   * @param names list of names, same order and length as usages
   * @return list of clb usages that were synced
   */
  Future<List<NameUsage>> sync(UUID datasetKey, ImporterCallback dao, List<NameUsage> usages, List<ParsedName> names);

  /**
   * @return list of clb usage keys that were updated
   */
  Future<List<Integer>> updateForeignKeys(UUID datasetKey, List<UsageForeignKeys> fks);

  /**
   * Delete all existing nub relations and then batch insert new ones from the passed map.
   * All dataset usages should be covered by the passed map and impossible nub matches should have a null value.
   * This will lead to NameUsageIssue.BACKBONE_MATCH_NONE being added to the usages issue set.
   *
   * This is a synchroneous call and on return all relations are guaranteed to be updated.
   *
   * @param datasetKey the datasource to map to the nub
   * @param relations  map from source usage id to a nub usage id for all usages in a dataset. Values can be null to indicate a NameUsageIssue.BACKBONE_MATCH_NONE
   */
  void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations);

  /**
   * Remove entire dataset from checklistbank
   * @return number of deleted usage records
   */
  int deleteDataset(UUID datasetKey);

  /**
   * @param usageKeys clb usage keys
   * @return same list of clb usage keys as input
   */
  Future<List<Integer>> deleteUsages(UUID datasetKey, List<Integer> usageKeys);

  /**
   * @return true if there is still a running import task
   */
  boolean isRunning();

}
