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
package org.gbif.nub.lookup;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.v2.NameUsageMatch2;
import org.gbif.api.vocabulary.Rank;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 *
 */
public interface NameUsageMatchingService2 extends NameUsageMatchingService {

  NameUsageMatch2 v2(NameUsageMatch m);

  /**
   * @param exclude set of higher taxon nub ids to exclude from matching results
   *
   */
  NameUsageMatch match2(String scientificName, @Nullable Rank rank, @Nullable LinneanClassification classification, Set<Integer> exclude, boolean strict, boolean verbose);

}
