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
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.model.UsageExtensions;

import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

public interface UsageSyncService {

  int syncUsage(boolean insert, NameUsage usage, ParsedName pn, NameUsageMetrics metrics);

  void syncUsageExtras(boolean insert, UUID datasetKey, int usageKey, @Nullable VerbatimNameUsage verbatim, @Nullable UsageExtensions extensions);

  void updateForeignKeys(int usageKey, Integer parentKey, Integer basionymKey);

  void insertNubRelations(UUID datasetKey, Map<Integer, Integer> relations);

  int deleteDataset(UUID datasetKey);

  void delete(int key);
}
