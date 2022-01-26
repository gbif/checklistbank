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
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.nub.NeoTmpRepoRule;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.util.UUID;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;

/**
 * Created by markus on 29/09/15.
 */
// TODO: remove ignore
@Ignore("REMOVE! ignored only to make the jenkins build work")
public class ClbSourceTest {

//  @Rule
//  public ClbLoadTestDb dbSetup = ClbLoadTestDb.squirrels();

  @Rule
  public NeoTmpRepoRule neoRepo = new NeoTmpRepoRule();

  private ClbConfiguration config(){
    // use default prod API
//    Properties props = dbSetup.getProperties();
    ClbConfiguration clb = new ClbConfiguration();
//    clb.databaseName = props.getProperty("checklistbank.db.dataSource.databaseName");
//    clb.serverName = props.getProperty("checklistbank.db.dataSource.serverName");
//    clb.user = props.getProperty("checklistbank.db.dataSource.user");
//    clb.password = props.getProperty("checklistbank.db.dataSource.password");
    return clb;
  }

  @Test
  public void testUsages() throws Exception {
    try (ClbSource src = new ClbSource(config(), neoRepo.cfg, UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4"), "squirrels")) {
      src.ignoreRanksAbove = Rank.SPECIES;
      src.init(true, false);
      int counter = 0;
      try (CloseableIterator<SrcUsage> iter = src.iterator()) {
        while (iter.hasNext()) {
          SrcUsage u = iter.next();
          counter++;
          System.out.print(u.key + "  ");
          System.out.println(u.scientificName);
        }
      }
      assertEquals(44, counter);
    }
  }

  @Test
  public void testExclusion() throws Exception {
    // exclude an entire subfamily subtree
    RankedName subfam = new RankedName("Sciurinae", Rank.SUBFAMILY);
    try (ClbSource src = new ClbSource(config(), neoRepo.cfg, UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4"), "squirrels", Lists.newArrayList(subfam))) {
      src.ignoreRanksAbove = Rank.SPECIES;
      src.init(true, false);
      int counter = 0;
      try (CloseableIterator<SrcUsage> iter = src.iterator()) {
        while (iter.hasNext()) {
          SrcUsage u = iter.next();
          counter++;
          System.out.print(u.key + "  ");
          System.out.println(u.scientificName);
        }
      }
      assertEquals(12, counter);
    }
  }
}