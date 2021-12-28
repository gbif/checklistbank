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
package org.gbif.checklistbank.iterable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
public class IntsTest {

  @Test
  public void testRange() throws Exception {
    int counter = 4;
    for (int i : Ints.range(4,8)) {
      assertEquals(counter++, i);
    }
    assertEquals(9, counter);

    counter = -12;
    for (int i : Ints.range(-12,0)) {
      assertEquals(counter++, i);
    }
    assertEquals(1, counter);

    boolean did12=false;
    for (int i : Ints.range(12,12)) {
      assertEquals(12, i);
      did12=true;
    }
    assertTrue(did12);
  }

  @Test
  public void testInvalidRange() {
    assertThrows(IllegalArgumentException.class, () -> Ints.range(12,8));
  }

  @Test
  public void testUntil() throws Exception {
    int counter = 1;
    for (int i : Ints.until(8)) {
      assertEquals(counter++, i);
    }
    assertEquals(9, counter);
  }
}