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

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.nub.NeoTmpRepoRule;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.utils.ResourcesMonitor;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.helpers.collection.Iterables;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;

public class ClasspathSourceListTest {

  @RegisterExtension
  public static NeoTmpRepoRule neoRepo = new NeoTmpRepoRule();

  @Test
  public void testListSources() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 1, 2, 3, 4, 5, 10, 11, 23, 12, 31);
    src.setSourceRank(23, Rank.KINGDOM);
    List<NubSource> sources = Iterables.asList(src);
    assertEquals(10, sources.size());
    assertEquals(Rank.FAMILY, sources.get(0).ignoreRanksAbove);
    assertEquals(Rank.KINGDOM, sources.get(7).ignoreRanksAbove);
  }


  /**
   * Test large amount of nub sources to see if neo4j / dao resources are managed properly
   */
  @Test
  @Disabled("Manual test to debug too many open files or other neo4j resource problems")
  public void testLargeLists() throws Exception {
    ResourcesMonitor monitor = new ResourcesMonitor();
    monitor.run();

    List<ClasspathSource> sources = Lists.newArrayList();
    // try 10 classpath sources 100 times = 10.000 sources!
    for (int rep = 0; rep < 10; rep++) {
      for (int id = 1; id < 100; id++) {
        sources.add(new ClasspathSource(id, false, neoRepo.cfg));
      }
      monitor.run();
    }
    ClasspathSourceList srcList = ClasspathSourceList.emptySource();
    srcList.submitSources(sources);
    monitor.run();
    for (NubSource src : srcList) {
      monitor.run();
      int counter = 0;
      try (CloseableIterator<SrcUsage> iter = src.iterator()) {
        while (iter.hasNext()) {
          SrcUsage u = iter.next();
          counter++;
        }
      }
      System.out.println(counter + " usages in source " + src.name);
      src.close();
    }
  }

}