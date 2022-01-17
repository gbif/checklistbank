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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.checklistbank.model.UsageRelated;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;
import org.springframework.stereotype.Repository;

/**
 * A generic MyBatis mapper for NameUsageComponent subclasses.
 *
 * @param <T> the interpreted model class.
 */
@Repository

public interface NameUsageComponentMapper<T> {

  List<T> listByChecklistUsage(@Param("key") int usageKey, @Param("page") Pageable page);

  List<T> listByNubUsage(@Param("key") int nubKey, @Param("page") Pageable page);

  List<UsageRelated<T>> listByChecklistUsageRange(@Param("start") int usageKeyStart, @Param("end") int usageKeyEnd);

  List<UsageRelated<T>> listByNubUsageRange(@Param("start") int usageKeyStart, @Param("end") int usageKeyEnd);

  void deleteByUsage(@Param("key") int usageKey);

  /**
   * Iterates over all components of a given dataset and processes them with the supplied handler.
   * This allows a single query to efficiently stream all its values without keeping them in memory.
   */
  Cursor<T> processDataset(@Param("uuid") UUID datasetKey);

}
