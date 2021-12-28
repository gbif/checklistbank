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


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SymmetricIdentityMatrixTest {

  @Test
  public void add() throws Exception {
    SymmetricIdentityMatrix<Integer> m = SymmetricIdentityMatrix.create();

    assertTrue(m.contains(1,1));
    assertFalse(m.contains(1,2));

    m.add(1,2);
    assertTrue(m.contains(1,1));
    assertTrue(m.contains(1,2));
    assertTrue(m.contains(2,1));
    assertFalse(m.contains(1,3));

    m.add(2,1);
    assertTrue(m.contains(1,1));
    assertTrue(m.contains(1,2));
    assertTrue(m.contains(2,1));
    assertFalse(m.contains(1,3));

    m.add(3,1);
    assertTrue(m.contains(1,1));
    assertTrue(m.contains(1,2));
    assertTrue(m.contains(2,1));
    assertTrue(m.contains(1,3));
    assertTrue(m.contains(3, 1));

    m.remove(2);
    assertTrue(m.contains(1,1));
    assertFalse(m.contains(1,2));
    assertFalse(m.contains(2,1));
    assertTrue(m.contains(2,2));
    assertTrue(m.contains(1,3));
    assertTrue(m.contains(3, 1));
  }

}