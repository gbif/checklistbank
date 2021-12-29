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
package org.gbif.nub.lookup.straight;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.utils.SerdeTestUtils;

import org.junit.jupiter.api.Test;

/**
 *
 */
public class LookupUsageMatchTest {

  @Test
  public void testSerde() throws Exception {
    LookupUsage u = new LookupUsage();
    u.setKey(321);
    u.setAuthorship("Mill.");
    u.setCanonical("Abies alba");
    u.setRank(Rank.UNRANKED);
    u.setKingdom(Kingdom.INCERTAE_SEDIS);
    u.setYear("1999");

    LookupUsageMatch m = new LookupUsageMatch();
    m.setMatch(u);

    SerdeTestUtils.testSerDe(m, LookupUsageMatch.class);
  }
}