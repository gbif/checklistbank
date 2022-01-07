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
package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.utils.text.StringUtils;

import java.util.Random;
import java.util.UUID;

/**
 * UsageSource implementation that works on classpath files for testing nub builds.
 * This class is just a test helper class, see NubBuilderTest for its use.
 * For every dataset source there needs to be simple flat usage tab file under resources/nub-sources
 * with the following columns:
 * This abstract class reads a tab delimited text stream expected with the following columns:
 * <ul>
 * <li>usageKey</li>
 * <li>parentKey</li>
 * <li>basionymKey</li>
 * <li>rank (enum)</li>
 * <li>taxonomicStatus (enum)</li>
 * <li>nomenclaturalStatus (enum[])</li>
 * <li>scientificName</li>
 * <li>namePublishedIn</li>
 * </ul>
 */
public class RandomSource extends NubSource {
  private int size;
  private final Kingdom kingdom;
  private Random rnd = new Random();

  public RandomSource(int size, Kingdom kingdom, NeoConfiguration neo) {
    super(UUID.randomUUID(), "Random dataset " + kingdom, null, true, neo);
    this.size = size;
    this.kingdom = kingdom;
  }

  @Override
  public void initNeo(NeoUsageWriter writer) throws Exception {
    // kingdom first
    String[] record = new String[]{"0", null, null, "KINGDOM", "ACCEPTED", null, kingdom.scientificName(), null};
    writer.addRow(record);
    size--;

    // link up families and species
    Integer familyID = null;
    Integer genusID = null;
    String genus = null;
    Integer speciesID = null;
    String species = null;
    while (size > 0) {
      int type = rnd.nextInt(1000);
      String publishedIn = StringUtils.randomString(40 + rnd.nextInt(120));
      if (familyID == null || type == 999) {
        // familyName
        familyID = size;
        record = new String[]{familyID.toString(), "0", null, "FAMILY", "ACCEPTED", null, fullName(StringUtils.randomFamily()), publishedIn};

      } else if (genusID == null || type > 950) {
        // species
        genusID = size;
        genus = StringUtils.randomGenus();
        record = new String[]{genusID.toString(), familyID.toString(), null, "GENUS", "ACCEPTED", null, fullName(genus), publishedIn};

      } else if (speciesID == null || type > 500) {
        // species
        speciesID = size;
        species = appendEpithet(genus);
        record = new String[]{speciesID.toString(), genusID.toString(), null, "SPECIES", "ACCEPTED", null, fullName(species), publishedIn};

      } else {
        // subspecies
        record = new String[]{String.valueOf(size), speciesID.toString(), null, "SUBSPECIES", "ACCEPTED", null, fullName(appendEpithet(species)), publishedIn};
      }
      writer.addRow(record);
      size--;
    }
  }

  private String appendEpithet(String name) {
    return name + " " + StringUtils.randomEpithet();
  }

  private String fullName(String canonical) {
    return canonical + " " + StringUtils.randomAuthor() + ", " + StringUtils.randomSpeciesYear();
  }
}
