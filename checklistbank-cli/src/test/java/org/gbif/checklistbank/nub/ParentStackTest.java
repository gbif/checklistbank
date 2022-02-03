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

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ParentStackTest {

  @Test
  public void testStack() throws Exception {
    NubUsage king = new NubUsage();
    king.kingdom = Kingdom.ANIMALIA;
    ParentStack parents = new ParentStack(king);

    assertEquals(0, parents.size());
    assertNotNull(parents.nubParent());

    parents.add(src(1, null));
    parents.add(src(2, 1));
    NubUsage nub = nub("nub#3");
    parents.put(nub);
    assertNotNull(parents.nubParent());

    assertFalse(parents.isDoubtful());
    parents.markSubtreeAsDoubtful(); // doubtful key=2
    assertTrue(parents.isDoubtful());

    parents.add(src(3, 2));
    assertEquals(3, parents.size());
    assertEquals(nub, parents.nubParent());
    assertTrue(parents.isDoubtful());

    parents.add(src(4, 1)); // this removes all but the first key
    assertEquals(2, parents.size());
    assertFalse(parents.isDoubtful());
    assertNotNull(parents.nubParent());
  }

  private SrcUsage src(int key, Integer parentKey) {
    SrcUsage u = new SrcUsage();
    u.key = key;
    u.parentKey = parentKey;
    u.scientificName = "Sciname #" + key;
    return u;
  }

  private NubUsage nub(String name) {
    NubUsage n = new NubUsage();
    n.parsedName = new ParsedName();
    n.parsedName.setScientificName(name);
    return n;
  }
}