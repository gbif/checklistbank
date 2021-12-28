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
package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntArrayPgWriterTest {

  @Test
  public void testWrite() throws Exception {
    assertInput("12\n54321\n23456789\n345670981");
    assertInput("12\n54321\n23456789\n345670981\n");
  }

  private void assertInput(final String input) throws Exception {
    IntArrayPgWriter writer = new IntArrayPgWriter();
    writer.write(input.toCharArray());
    List<Integer> result = writer.result();
    assertEquals(4, result.size());
    assertTrue(result.contains(12));
    assertTrue(result.contains(54321));
    assertTrue(result.contains(23456789));
    assertTrue(result.contains(345670981));
  }
}
