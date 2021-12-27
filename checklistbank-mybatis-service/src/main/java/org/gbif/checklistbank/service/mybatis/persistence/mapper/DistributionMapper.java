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

import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.checklistbank.model.IucnRedListCategory;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;


/**
 * The MyBatis mapper interface for Distribution.
 */
@Repository
public interface DistributionMapper extends NameUsageComponentMapper<Distribution> {
  UUID iucnDatasetKey = UUID.fromString("19491596-35ae-4a91-9a98-85cf505f1bd3");

  void insert(@Param("key") int usageKey, @Param("obj") Distribution distribution, @Param("sourceKey") Integer sourceKey);

  IucnRedListCategory getIucnRedListCategory(@Param("key") int nubKey);

}
