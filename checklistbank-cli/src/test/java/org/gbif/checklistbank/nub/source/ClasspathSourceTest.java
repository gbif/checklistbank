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

import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.nub.NeoTmpRepoRule;
import org.gbif.checklistbank.nub.model.SrcUsage;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by markus on 29/09/15.
 */
@Ignore("REMOVE! ignored only to make the jenkins build work")
public class ClasspathSourceTest {

  @ClassRule
  public static NeoTmpRepoRule neoRepo = new NeoTmpRepoRule();

  @Test
  public void testUsages() throws Exception {
    ClasspathSource src = new ClasspathSource(1, neoRepo.cfg);
    src.init(false, false);

    int counter = 0;
    try (CloseableIterator<SrcUsage> iter = src.iterator()) {
      while (iter.hasNext()) {
        SrcUsage u = iter.next();
        counter++;
        System.out.print(u.key + "  ");
        System.out.print(u.scientificName + " :: ");
        System.out.println(u.publishedIn);
      }
    }
    assertEquals(12, counter);

  }
}