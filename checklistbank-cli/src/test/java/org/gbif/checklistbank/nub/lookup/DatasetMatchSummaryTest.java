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
package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.vocabulary.Rank;

import java.util.UUID;

import org.junit.Test;

public class DatasetMatchSummaryTest {
  @Test
  public void toStringTest() throws Exception {
    DatasetMatchSummary summary = new DatasetMatchSummary(UUID.randomUUID());
    summary.addNoMatch(Rank.KINGDOM);
    summary.addNoMatch(Rank.CLASS);
    summary.addNoMatch(Rank.CLASS);
    summary.addNoMatch(Rank.FORM);
    summary.addNoMatch(Rank.BIOVAR);
    summary.addNoMatch(Rank.SUBGENUS);
    summary.addNoMatch(Rank.UNRANKED);
    summary.addNoMatch(null);

    summary.addMatch(Rank.ORDER);
    summary.addMatch(Rank.FAMILY);
    summary.addMatch(Rank.GENUS);
    summary.addMatch(Rank.SPECIES);
    summary.addMatch(Rank.VARIETY);
    summary.addMatch(Rank.SPECIES);
    summary.addMatch(Rank.SUBSPECIES);
    summary.addMatch(null);

    System.out.println(summary);
  }

}
