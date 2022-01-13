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
package org.gbif.checklistbank.cli.common;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.vocabulary.Rank;

import javax.annotation.Nullable;

/**
 * Mock matching service configured to always return none matches.
 */
public class NoneMatchingService implements NameUsageMatchingService {

  @Override
  public NameUsageMatch match(String scientificName, @Nullable Rank rank,
    @Nullable LinneanClassification classification, boolean strict, boolean verbose) {

    NameUsageMatch match = new NameUsageMatch();
    match.setConfidence(0);
    match.setMatchType(NameUsageMatch.MatchType.NONE);
    return match;
  }
}
