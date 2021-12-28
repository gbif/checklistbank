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
package org.gbif.checklistbank.model;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EqualityTest {

  @Test
  public void testAnd() throws Exception {
    assertEquals(Equality.EQUAL, Equality.EQUAL.and(Equality.EQUAL));
    assertEquals(Equality.EQUAL, Equality.EQUAL.and(Equality.UNKNOWN));
    assertEquals(Equality.EQUAL, Equality.UNKNOWN.and(Equality.EQUAL));

    assertEquals(Equality.UNKNOWN, Equality.UNKNOWN.and(Equality.UNKNOWN));

    assertEquals(Equality.DIFFERENT, Equality.DIFFERENT.and(Equality.DIFFERENT));
    assertEquals(Equality.DIFFERENT, Equality.UNKNOWN.and(Equality.DIFFERENT));
    assertEquals(Equality.DIFFERENT, Equality.DIFFERENT.and(Equality.UNKNOWN));
    assertEquals(Equality.DIFFERENT, Equality.EQUAL.and(Equality.DIFFERENT));
    assertEquals(Equality.DIFFERENT, Equality.DIFFERENT.and(Equality.EQUAL));
  }
}