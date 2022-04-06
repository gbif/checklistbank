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
package org.gbif.nub.lookup.fuzzy;

import org.gbif.api.model.checklistbank.ParsedName;

import org.gbif.nameparser.api.Rank;
import org.junit.jupiter.api.Test;

import static org.gbif.api.vocabulary.Rank.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NubMatchingServiceImplTest {

  @Test
  public void testInterpretGenus() throws Exception {
    ParsedName pn = new ParsedName();
    pn.setGenusOrAbove("P.");
    pn.setSpecificEpithet("concolor");
    NubMatchingServiceImpl.interpretGenus(pn, "Puma");
    assertEquals("Puma concolor", pn.canonicalName());


    pn.setGenusOrAbove("P.");
    NubMatchingServiceImpl.interpretGenus(pn, "Felis");
    assertEquals("P. concolor", pn.canonicalName());
  }

  @Test
  public void rankSimilarity() throws Exception {
    assertEquals(6, NubMatchingServiceImpl.rankSimilarity(FAMILY, FAMILY));
    assertEquals(6, NubMatchingServiceImpl.rankSimilarity(SPECIES, SPECIES));
    assertEquals(-1, NubMatchingServiceImpl.rankSimilarity(GENUS, SUBGENUS));
    assertEquals(2, NubMatchingServiceImpl.rankSimilarity(SPECIES, SPECIES_AGGREGATE));
    assertEquals(6, NubMatchingServiceImpl.rankSimilarity(UNRANKED, UNRANKED));
    assertEquals(-1, NubMatchingServiceImpl.rankSimilarity(UNRANKED, null));
    assertEquals(0, NubMatchingServiceImpl.rankSimilarity(FAMILY, UNRANKED));
    assertEquals(0, NubMatchingServiceImpl.rankSimilarity(SPECIES, UNRANKED));
    assertEquals(-9, NubMatchingServiceImpl.rankSimilarity(SUBSPECIES, VARIETY));
    assertEquals(2, NubMatchingServiceImpl.rankSimilarity(SUBSPECIES, INFRASPECIFIC_NAME));
    assertEquals(-35, NubMatchingServiceImpl.rankSimilarity(GENUS, org.gbif.api.vocabulary.Rank.CLASS));
    assertEquals(-35, NubMatchingServiceImpl.rankSimilarity(GENUS, FAMILY));
    assertEquals(-28, NubMatchingServiceImpl.rankSimilarity(FAMILY, KINGDOM));
  }

  @Test
  public void testNormConfidence2() throws Exception {
    for (int x=80; x<150; x++) {
      System.out.println(x + " -> " + NubMatchingServiceImpl.normConfidence(x));
    }
  }

  @Test
  public void testNormConfidence() throws Exception {
    assertEquals(0, NubMatchingServiceImpl.normConfidence(0));
    assertEquals(0, NubMatchingServiceImpl.normConfidence(-1));
    assertEquals(0, NubMatchingServiceImpl.normConfidence(-10000));
    assertEquals(1, NubMatchingServiceImpl.normConfidence(1));
    assertEquals(10, NubMatchingServiceImpl.normConfidence(10));
    assertEquals(20, NubMatchingServiceImpl.normConfidence(20));
    assertEquals(30, NubMatchingServiceImpl.normConfidence(30));
    assertEquals(50, NubMatchingServiceImpl.normConfidence(50));
    assertEquals(60, NubMatchingServiceImpl.normConfidence(60));
    assertEquals(70, NubMatchingServiceImpl.normConfidence(70));
    assertEquals(80, NubMatchingServiceImpl.normConfidence(80));
    assertEquals(85, NubMatchingServiceImpl.normConfidence(85));
    assertEquals(88, NubMatchingServiceImpl.normConfidence(90));
    assertEquals(89, NubMatchingServiceImpl.normConfidence(92));
    assertEquals(91, NubMatchingServiceImpl.normConfidence(95));
    assertEquals(92, NubMatchingServiceImpl.normConfidence(98));
    assertEquals(92, NubMatchingServiceImpl.normConfidence(99));
    assertEquals(93, NubMatchingServiceImpl.normConfidence(100));
    assertEquals(95, NubMatchingServiceImpl.normConfidence(105));
    assertEquals(96, NubMatchingServiceImpl.normConfidence(110));
    assertEquals(97, NubMatchingServiceImpl.normConfidence(115));
    assertEquals(99, NubMatchingServiceImpl.normConfidence(120));
    assertEquals(100, NubMatchingServiceImpl.normConfidence(125));
    assertEquals(100, NubMatchingServiceImpl.normConfidence(130));
    assertEquals(100, NubMatchingServiceImpl.normConfidence(150));
    assertEquals(100, NubMatchingServiceImpl.normConfidence(175));
    assertEquals(100, NubMatchingServiceImpl.normConfidence(200));
    assertEquals(100, NubMatchingServiceImpl.normConfidence(1000));
  }

}
