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

import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.paging.Pageable;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * The MyBatis mapper interface for Identifier.
 * It does not extend the NameUsageComponentMapper cause nub and checklist usages are treated the same
 * for identifiers, i.e. all nub identifiers hang of the nub usage directly.
 */
@Repository
public interface IdentifierMapper {

  Identifier get(@Param("key") int key);

  List<Identifier> listByUsage(@Param("key") int usageKey, @Param("page") Pageable page);

  void deleteByUsage(@Param("key") int usageKey);

  void insert(@Param("key") int usageKey, @Param("obj") Identifier identifier);
}
