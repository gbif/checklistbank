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
package org.gbif.checklistbank.nub.validation;

import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;

import javax.annotation.Nullable;

public interface AssertionEngine {

  boolean isValid();

  void assertUsage(int usageKey, Rank rank, String name, @Nullable String accepted, Kingdom kingdom);


  void assertParentsContain(String searchName, Rank searchRank, String parent);

  void assertParentsContain(int usageKey, Rank parentRank, String parent);


  void assertClassification(int usageKey, LinneanClassification classification);

  void assertClassification(int usageKey, String... classification);


  void assertSearchMatch(int expectedSearchMatches, String name);

  void assertSearchMatch(int expectedSearchMatches, String name, Rank rank);

  void assertNotExisting(String name, Rank rank);

}
