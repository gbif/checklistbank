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

import org.gbif.api.vocabulary.Rank;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.beust.jcommander.internal.Lists;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;

import static org.gbif.api.vocabulary.Kingdom.*;
import static org.gbif.api.vocabulary.TaxonomicStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
public class LookupUsageTest {

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
  public void testStr() throws Exception {
    System.out.println(
      new LookupUsage(443, map(1,2, 3,4), "Abies", "Mill.", "1978", Rank.GENUS, ACCEPTED, PLANTAE, false).toString()
    );
  }

  @Test
  public void testCompareTo() throws Exception {
    List<LookupUsage> usages = Lists.newArrayList();
    usages.add(new LookupUsage(443, "Abies", "Mill.", "1978", Rank.GENUS, ACCEPTED, PLANTAE, false));
    usages.add(new LookupUsage(13, "Abies milba", "Mill.", "1978", Rank.SPECIES, ACCEPTED, PLANTAE, false));
    usages.add(new LookupUsage(3, "Abies alba", "Mill.", "1978", Rank.SPECIES, ACCEPTED, PLANTAE, false));
    usages.add(new LookupUsage(5323, "Papaia", "L.", "1978", Rank.GENUS, ACCEPTED, ANIMALIA, false));
    usages.add(new LookupUsage(23, "Abia giganta", "Mill.", "1978", Rank.SPECIES, ACCEPTED, ANIMALIA, false));
    usages.add(new LookupUsage(113, "Keine AHnung", null, null, Rank.UNRANKED, ACCEPTED, INCERTAE_SEDIS, true));
    usages.add(new LookupUsage(88, "Keiner", null, null, Rank.SUPERFAMILY, ACCEPTED, INCERTAE_SEDIS, true));

    Collections.sort(usages);

    assertEquals(88, usages.get(0).getKey());
    assertEquals(443, usages.get(1).getKey());
    assertEquals(5323, usages.get(2).getKey());
    assertEquals(23, usages.get(3).getKey());
    assertEquals(3, usages.get(4).getKey());
    assertEquals(13, usages.get(5).getKey());
    assertEquals(113, usages.get(6).getKey());
  }
}