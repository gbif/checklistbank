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

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.Rank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NameFormatterTest {

  @Test
  public void scientificName() throws Exception {
    final String UNPARSED_NAME = "A real unparsed name X73a";
    ParsedName n = new ParsedName();
    n.setType(NameType.SCIENTIFIC);
    n.setScientificName(UNPARSED_NAME);
    n.setGenusOrAbove("Abies");
    n.setAuthorship("L.");
    n.setYear("1888");
    n.setBracketAuthorship("Carl.");
    n.setRank(Rank.GENUS);

    assertAllButVirus("Abies (Carl.) L., 1888", n);

    n.setRank(Rank.SPECIES);
    assertAllButVirus("Abies spec.", n);

    n.setSpecificEpithet("alba");
    assertAllButVirus("Abies alba (Carl.) L., 1888", n);

    n.setRank(Rank.SUBSPECIES);
    assertAllButVirus("Abies alba subsp.", n);

    n.setInfraSpecificEpithet("alpina");
    final String expectedWithRank = "Abies alba subsp. alpina (Carl.) L., 1888";
    assertAllAndZoo("Abies alba alpina (Carl.) L., 1888", expectedWithRank, n);
    assertEquals("Abies alba alpina (Carl.) L., 1888", NameFormatter.scientificName(n, Kingdom.ANIMALIA));
    assertEquals(expectedWithRank, NameFormatter.scientificName(n, Kingdom.PLANTAE));

    n.setType(NameType.HYBRID);
    assertEquals(expectedWithRank, NameFormatter.scientificName(n, NomenclaturalCode.BOTANICAL));

    n.setType(NameType.VIRUS);
    assertEquals(expectedWithRank, NameFormatter.scientificName(n, NomenclaturalCode.BOTANICAL));

    n.setParsed(false);
    assertEquals(UNPARSED_NAME, NameFormatter.scientificName(n, NomenclaturalCode.BOTANICAL));

    n.setParsed(true);
    n.setRank(Rank.INFRASPECIFIC_NAME);
    assertAllButVirus("Abies alba alpina (Carl.) L., 1888", n);

    assertEquals(UNPARSED_NAME, NameFormatter.scientificName(n, Kingdom.VIRUSES));
  }

  private static void assertAllButVirus(String expected, ParsedName pn) {
    for (NomenclaturalCode code : NomenclaturalCode.values()) {
      if (code == NomenclaturalCode.VIRUS) continue;
      assertEquals(expected, NameFormatter.scientificName(pn, code));
    }
  }

  private static void assertAllAndZoo(String zoo, String other, ParsedName pn) {
    assertEquals(zoo, NameFormatter.scientificName(pn, NomenclaturalCode.ZOOLOGICAL));
    for (NomenclaturalCode code : NomenclaturalCode.values()) {
      if (code == NomenclaturalCode.ZOOLOGICAL || code == NomenclaturalCode.VIRUS) continue;
      assertEquals(other, NameFormatter.scientificName(pn, code));
    }
  }

}