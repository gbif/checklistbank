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
package org.gbif.checklistbank.nub;

import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.nub.lookup.straight.LookupUsage;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.junit.Test;

import static org.gbif.api.vocabulary.Kingdom.ANIMALIA;
import static org.gbif.api.vocabulary.Kingdom.PLANTAE;
import static org.gbif.api.vocabulary.Kingdom.VIRUSES;
import static org.gbif.api.vocabulary.Rank.*;
import static org.gbif.api.vocabulary.TaxonomicStatus.ACCEPTED;
import static org.gbif.api.vocabulary.TaxonomicStatus.DOUBTFUL;
import static org.gbif.api.vocabulary.TaxonomicStatus.SYNONYM;

import static org.junit.Assert.assertEquals;

public class IdGeneratorTest {

  public static IdLookup newTestLookup() {
    Collection<LookupUsage> usages = Lists.newArrayList(
        new LookupUsage(1, "Animalia", null, null, KINGDOM, null, ANIMALIA, false),
        new LookupUsage(2, "Oenanthe", "Vieillot", "1816", GENUS, null, ANIMALIA, false),
        new LookupUsage(3, "Oenanthe", "Linnaeus", "1753", GENUS, null, PLANTAE, false),
        new LookupUsage(4, "Oenanthe aquatica", "Poir.", null, SPECIES, null, PLANTAE, false),
        new LookupUsage(5, "Oenanthe aquatica", "Senser", "1957", SPECIES, null, PLANTAE, false),
        new LookupUsage(6, "Oenanthe aquatica", null, null, SPECIES, null, PLANTAE, true),
        new LookupUsage(7, "Rodentia", "Bowdich", "1821", ORDER, null, ANIMALIA, false),
        new LookupUsage(8, "Rodentia", null, null, GENUS, null, ANIMALIA, true),
        new LookupUsage(9, "Abies alba", null, null, SPECIES, null, PLANTAE, false),
        new LookupUsage(10, "Abies alba", "Mumpf.", null, SPECIES, null, PLANTAE, true),
        new LookupUsage(11, "Abies alba", null, "1778", SPECIES, null, PLANTAE, true),
        new LookupUsage(12, "Picea alba", null, "1778", SPECIES, null, PLANTAE, true),
        new LookupUsage(13, "Picea", null, null, GENUS, null, PLANTAE, true),
        new LookupUsage(14, "Carex cayouettei", null, null, SPECIES, null, PLANTAE, true),
        new LookupUsage(15, "Carex comosa × Carex lupulina", null, null, SPECIES, null, PLANTAE, true),
        new LookupUsage(16, "Aeropyrum coil-shaped virus", null, null, UNRANKED, null, VIRUSES, true),

        new LookupUsage(17, map(100, 17, 110, -18, 111, 19),  "Admetidae", "Troschel", "1865", FAMILY, null, ANIMALIA, false),
        new LookupUsage(20, null, "Admetidae", null, null, FAMILY, null, ANIMALIA, true),
        new LookupUsage(5093664, map(5093663, 5093664, 1673124, -8431281, 1673124, 8710209),  "Bombylius scintillans", "Brunetti", "1909", SPECIES, null, ANIMALIA, false)
    );
    return IdLookupImpl.temp().load(usages);
  }

  public static IdLookup newMimusLookup() {
    Collection<LookupUsage> usages = Lists.newArrayList(
            new LookupUsage(1, "Animalia", null, null, KINGDOM, null, ANIMALIA, false),
            new LookupUsage(9498764, "Mimus", null, null, GENUS, ACCEPTED, ANIMALIA, false),
            new LookupUsage(9803710, "Mimus", null, null, GENUS, DOUBTFUL, ANIMALIA, true),
            new LookupUsage(2494919, "Mimus", "Boie", "1826", GENUS, DOUBTFUL, ANIMALIA, false),
            new LookupUsage(9032015, "Mimus", "Boie, F,", "1826", GENUS, ACCEPTED, ANIMALIA, true),
            new LookupUsage(8347560, "Mimus", "Fåhraeus", "1871", GENUS, SYNONYM, ANIMALIA, true),
            // Pseudofentonia gen. Mimus gets badly parsed into genus Mimus
            new LookupUsage(1175552, "Mimus", null, null, GENUS, DOUBTFUL, ANIMALIA, false),
            // Pseudofentonia (Mimus)
            new LookupUsage(9633312, "Mimus", null, null, GENUS, DOUBTFUL, ANIMALIA, true)

    );
    return IdLookupImpl.temp().load(usages);
  }
  /**
   * key, value, key, value, ...
   * pro parte maps have the parent usageKey as key, the pro parte usage key as value
   */
  public static Int2IntMap map(int ... kvs) {
    Int2IntMap m = new Int2IntArrayMap(kvs.length / 2);
    int idx = 0;
    while (idx < kvs.length) {
      m.put(kvs[idx], kvs[idx+1]);
      idx = idx + 2;
    }
    return m;
  }

  @Test
  public void testIssueId() throws Exception {
    IdGenerator gen = new IdGenerator(newTestLookup(), 1000);

    File dir = FileUtils.createTempDir();
    try {
      gen.writeReports(dir);

      assertEquals(1000, gen.issue("Dracula", null, null, GENUS, null, PLANTAE));
      assertEquals(1, gen.issue("Animalia", null, null, KINGDOM, null, ANIMALIA));
      assertEquals(8, gen.issue("Rodentia", null, null, GENUS, null, ANIMALIA));
      // external issueing
      gen.reissue(14);
      // was issued already!
      assertEquals(1001, gen.issue("Carex cayouettei", null, null, SPECIES, null, PLANTAE));
      assertEquals(1002, gen.issue("Animalia", null, null, KINGDOM, null, ANIMALIA));
      assertEquals(1003, gen.issue("Carex cayouettei", null, null, SPECIES, null, PLANTAE));

      FileUtils.deleteDirectoryRecursively(dir);
      gen.writeReports(dir);


    } finally {
      FileUtils.deleteDirectoryRecursively(dir);
    }
  }

  /**
   * https://github.com/gbif/checklistbank/issues/154
   */
  @Test
  public void testMimus() throws IOException, InterruptedException {
    IdGenerator gen = new IdGenerator(newMimusLookup(), 10000000);
    assertEquals(9498764, gen.issue("Mimus", null, null, GENUS, ACCEPTED, ANIMALIA));
    assertEquals(2494919, gen.issue("Mimus", "F.Boie", "1826", GENUS, DOUBTFUL, ANIMALIA));
  }

  @Test
  public void testDuplicates() throws Exception {
    // if we had multiple ids for the same canonical in the past we can get them all out again
    IdGenerator gen = new IdGenerator(newTestLookup(), 1000);
    // regular canonical match
    assertEquals(20, gen.issue("Admetidae", null, null, FAMILY, null, ANIMALIA));
    // this returns now "Troschel", but considered a match
    assertEquals(17, gen.issue("Admetidae", null, null, FAMILY, null, ANIMALIA));
    // only now we issue a new id
    assertEquals(1000, gen.issue("Admetidae", null, null, FAMILY, null, ANIMALIA));
  }

  @Test
  public void testProParte() throws Exception {
    IdGenerator gen = new IdGenerator(newTestLookup(), 1000);
    // wrong kingdom
    assertEquals(1000, gen.issue("Admetidae", null, null, FAMILY, null, PLANTAE));
    // regular canonical match
    assertEquals(20, gen.issue("Admetidae", null, null, FAMILY, null, ANIMALIA));
    // pro parte matching
    assertEquals(17, gen.issue("Admetidae", "Troschel", null, FAMILY, null, ANIMALIA, 100));
    assertEquals(1001, gen.issue("Admetidae", "Troschel", null, FAMILY, null, ANIMALIA, 100));
    // deleted, but reissued
    assertEquals(18, gen.issue("Admetidae", "Troschel", null, FAMILY, null, ANIMALIA, 110));
    assertEquals(19, gen.issue("Admetidae", "Troschel", null, FAMILY, null, ANIMALIA, 111));
    assertEquals(1002, gen.issue("Admetidae", "Troschel", null, FAMILY, null, ANIMALIA, 200));

    assertEquals(8710209, gen.issue("Bombylius scintillans", "Brunetti", "1909", SPECIES, null, ANIMALIA, 1673124));
    assertEquals(5093664, gen.issue("Bombylius scintillans", "Brunetti", "1909", SPECIES, null, ANIMALIA));
  }

}