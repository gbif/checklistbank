/***************************************************************************
 * Copyright 2010 Global Biodiversity Information Facility Secretariat
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ***************************************************************************/

package org.gbif.nub.lookup;

import org.gbif.api.vocabulary.Rank;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author markus
 */
public class HigherTaxaComparatorTest {

  @Test
  public void testReloading() throws IOException {
    HigherTaxaComparator htl = new HigherTaxaComparator();
    htl.loadClasspathDicts("dicts");
    assertTrue(htl.size() > 10);
  }

  @Test
  public void testKingdoms() throws IOException {
    HigherTaxaComparator htl = new HigherTaxaComparator();
    htl.loadClasspathDicts("dicts");
    // Animalia varieties
    assertEquals("Animalia", htl.lookup("Animalia", Rank.KINGDOM));
    assertEquals("Animalia", htl.lookup("Anamalia", Rank.KINGDOM));
    assertEquals("Animalia", htl.lookup("Animal", Rank.KINGDOM));
    assertEquals("Animalia", htl.lookup("Metazoa", Rank.KINGDOM));

    assertNull(htl.lookup("Incertae sedis", Rank.KINGDOM));
  }

  @Test
  public void testBlacklist() throws IOException {
    HigherTaxaComparator htl = new HigherTaxaComparator();
    htl.loadClasspathDicts("dicts");

    assertFalse(htl.isBlacklisted("Animals"));
    assertFalse(htl.isBlacklisted("Abies indeterminata"));
    assertTrue(htl.isBlacklisted("Unknown"));
    assertTrue(htl.isBlacklisted("Incertae sedis"));
  }

  @Test
  public void testNormalization() throws IOException {
    assertEquals("ANIMALS", HigherTaxaComparator.norm("Animals"));
    assertEquals("ANIMALS", HigherTaxaComparator.norm("  Animals"));
    assertEquals("ANIMALS", HigherTaxaComparator.norm("12Animals???"));
    assertEquals("DRECKS ASTERACEAE", HigherTaxaComparator.norm("drecks Asteraceae"));
    assertNull(HigherTaxaComparator.norm(null));
    assertNull(HigherTaxaComparator.norm(" "));
    assertNull(HigherTaxaComparator.norm("321"));
    assertNull(HigherTaxaComparator.norm(""));
    assertNull(HigherTaxaComparator.norm(",.-öä? "));
  }

}
