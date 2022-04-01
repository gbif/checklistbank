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

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsageMapper {

  void delete(@Param("key") int key);

  void deleteLogically(@Param("key") int key);

  int deleteByDataset(@Param("uuid") UUID datasetKey);

  List<Integer> listByDatasetAndDate(@Param("uuid") UUID datasetKey, @Param("before") Date before);

  /**
   * Return ids of all parents, limited to max 100 to avoid endless loops that bring down the JVM
   * as seen during CoL index build
   */
  List<Integer> listParents(@Param("key") int usageKey);

  /**
   * Update a backbone name usage with the given source taxon key
   */
  void updateSourceTaxonKey(@Param("key") int nubKey, @Param("sourceTaxonKey") int sourceTaxonKey);

  /**
   * Sets sourceTaxonKey to null for all backbone name usages from a given constituent
   */
  void deleteSourceTaxonKeyByConstituent(@Param("uuid") UUID constituentKey);

}
