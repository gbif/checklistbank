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

import org.gbif.checklistbank.model.NubMapping;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;
import org.springframework.stereotype.Repository;

@Repository
public interface NubRelMapper {

  void insert(@Param("uuid") UUID datasetKey, @Param("usageKey") int usageKey, @Param("nubKey") int nubKey);

  void delete(@Param("usageKey") int usageKey);

  void deleteByDataset(@Param("uuid") UUID datasetKey);

  Cursor<NubMapping> process(@Param("uuid") UUID datasetKey);
}
