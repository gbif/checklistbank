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
package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.v2.RankedName;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.checklistbank.model.NameUsageWritable;
import org.gbif.checklistbank.model.ParsedNameUsage;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;
import org.springframework.stereotype.Repository;

/**
 * The MyBatis mapper interface for NameUsages.
 *
 * @see org.gbif.api.service.checklistbank.NameUsageService
 */
@Repository
public interface NameUsageMapper {

  NameUsage get(@Param("key") int key);

  /**
   * Returns the existing usage key for a given taxonID in a dataset or null if its not existing.
   * In case more than one usage exists with the given taxonID (which should never happen, these are invalid datasets
   * with non unique keys) the first key is selected.
   */
  Integer getKey(@Param("uuid") UUID datasetKey, @Param("taxonId") String taxonId);

  /**
   * A simple paging query for all non deleted usages in checklistbank.
   * We only return name usage ids here to avoid extremely heavy operations for the database when the offset gets
   * bigger.
   *
   * @return the name usage ids belonging to the requested page.
   */
  List<Integer> list(@Nullable @Param("uuid") UUID datasetKey, @Param("page") Pageable page);

  /**
   * @return the number of non deleted name usages
   */
  int count(@Nullable @Param("uuid") UUID datasetKey);

  List<NameUsage> listByTaxonId(
      @Param("uuid") UUID datasetKey, @Param("taxonId") String taxonId, @Param("page") Pageable page);

  List<NameUsage> listByCanonicalName(@Param("canonical") String canonicalName, @Param("uuids") UUID[] datasetKey,
                                      @Param("page") Pageable page);


  List<NameUsage> listRoot(@Param("uuid") UUID datasetKey, @Param("page") Pageable page);

  List<NameUsage> listChildren(@Param("key") int parentKey, @Param("page") Pageable page);

  List<NameUsage> listSynonyms(@Param("key") int usageKey, @Param("page") Pageable page);

  List<NameUsage> listCombinations(@Param("key") int basionymKey);

  List<NameUsage> listRange(@Param("start") int usageKeyStart, @Param("end") int usageKeyEnd);

  /**
   * Iterates over all name usages of a given dataset and processes them with the supplied handler.
   * This allows a single query to efficiently stream all its values without keeping them in memory.
   */
  Cursor<ParsedNameUsage> processDataset(@Param("uuid") UUID datasetKey);

  /**
   * Iterates over all usage names and processes them with the supplied handler.
   * This allows a single query to efficiently stream all its values without keeping them in memory.
   */
  Cursor<RankedName> processAllNames();

  /**
   * List all related name usages that have a given nubKey.
   */
  List<NameUsage> listRelated(@Param("key") int nubKey, @Param("uuids") UUID[] datasetKeys, @Param("page") Pageable page);

  /**
   * @return the maximum usage key used in a dataset incl deleted records
   */
  Integer maxUsageKey(@Param("uuid") UUID datasetKey);

  /**
   * Insert a new name usage, setting lastInterpretedDate to current date and assigning a new usage key.
   * If higher rank keys like kingdomKey are -1 this is interpreted that they should point to the newly inserted record
   * itself, inserting the newly generated usage key for those negative properties.
   */
  void insert(@Param("u") NameUsageWritable usage);

  /**
   * Updates an existing name usage.
   * See #insert(NameUsageWritable usage) method for details on behavior.
   */
  void update(@Param("u") NameUsageWritable usage);

  void updateName(@Param("key") int usageKey, @Param("nkey") int nameKey);

  void updateForeignKeys(@Param("key") int usageKey, @Param("par") Integer parentKey, @Param("bas") Integer basionymKey);

  /**
   * @return the set of issues associated with the usage
   */
  NameUsage getIssues(@Param("key") int usageKey);

  /**
   * @return list all usage keys that have a BACKBONE_MATCH_NONE issue.
   */
  Set<Integer> listNoMatchUsageKeys(@Param("uuid") UUID datasetKey);

  /**
   * Update the name usage issues with the given set
   */
  void updateIssues(@Param("key") int usageKey, @Param("issues") Set<NameUsageIssue> issues);

  void addIssue(@Param("key") int usageKey, @Param("issue") NameUsageIssue issue);

  void removeIssue(@Param("key") int usageKey, @Param("issue") NameUsageIssue issue);

}
