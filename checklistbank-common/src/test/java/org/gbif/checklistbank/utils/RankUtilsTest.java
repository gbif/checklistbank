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
package org.gbif.checklistbank.utils;

import org.gbif.api.vocabulary.Rank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RankUtilsTest {

  @Test
  public void linneanBaseRank() throws Exception {
    for (Rank r : Rank.values()) {
      assertNotNull(RankUtils.linneanBaseRank(r));
    }
    assertEquals(Rank.ORDER, RankUtils.linneanBaseRank(Rank.MAGNORDER));
    assertEquals(Rank.FAMILY, RankUtils.linneanBaseRank(Rank.SUPERFAMILY));
    assertEquals(Rank.FAMILY, RankUtils.linneanBaseRank(Rank.SUBFAMILY));
    assertEquals(Rank.GENUS, RankUtils.linneanBaseRank(Rank.SUBGENUS));
  }

  @Test
  public void testNextLowerLinneanRank() throws Exception {
    assertEquals(Rank.SPECIES, RankUtils.nextLowerLinneanRank(Rank.GENUS));
    assertEquals(Rank.GENUS, RankUtils.nextLowerLinneanRank(Rank.SUBFAMILY));
    assertEquals(Rank.SPECIES, RankUtils.nextLowerLinneanRank(Rank.SUBGENUS));
    assertEquals(Rank.PHYLUM, RankUtils.nextLowerLinneanRank(Rank.KINGDOM));
    assertEquals(Rank.KINGDOM, RankUtils.nextLowerLinneanRank(Rank.DOMAIN));
    assertEquals(Rank.SPECIES, RankUtils.nextLowerLinneanRank(Rank.INFRAGENERIC_NAME));
    assertEquals(null, RankUtils.nextLowerLinneanRank(Rank.INFRASUBSPECIFIC_NAME));
    assertEquals(null, RankUtils.nextLowerLinneanRank(Rank.VARIETY));
  }

  @Test
  public void testNextHigherLinneanRank() throws Exception {
    assertEquals(Rank.FAMILY, RankUtils.nextHigherLinneanRank(Rank.GENUS));
    assertEquals(Rank.FAMILY, RankUtils.nextHigherLinneanRank(Rank.SUBFAMILY));
    assertEquals(Rank.GENUS, RankUtils.nextHigherLinneanRank(Rank.SUBGENUS));
    assertEquals(null, RankUtils.nextHigherLinneanRank(Rank.KINGDOM));
    assertEquals(null, RankUtils.nextHigherLinneanRank(Rank.DOMAIN));
    assertEquals(Rank.SPECIES, RankUtils.nextHigherLinneanRank(Rank.VARIETY));
  }
}