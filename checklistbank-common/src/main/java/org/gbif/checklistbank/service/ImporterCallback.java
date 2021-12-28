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

import java.util.List;
import java.util.concurrent.Future;

/**
 *
 */
public interface ImporterCallback {

  /**
   * Return the usage for the given (neo4j node) id.
   * There can be multiple usages in case of the (rare) pro parte synonyms.
   */
  NameUsage readUsage(long id);

  ParsedName readName(long id);

  boolean isInsert(NameUsage usage);

  UsageExtensions readExtensions(long id);

  NameUsageMetrics readMetrics(long id);

  VerbatimNameUsage readVerbatim(long id);

  List<Integer> readParentKeys(long id);

  void reportUsageKey(long id, int usageKey);

  void reportNewFuture(Future<List<Integer>> future);
}
