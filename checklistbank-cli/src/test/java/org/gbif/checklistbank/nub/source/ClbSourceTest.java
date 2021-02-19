package org.gbif.checklistbank.nub.source;

import com.google.common.collect.Lists;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.nub.NeoTmpRepoRule;
import org.gbif.checklistbank.nub.NubBuilderIT;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by markus on 29/09/15.
 */
public class ClbSourceTest {

  @Rule
  public ClbDbTestRule dbSetup = ClbDbTestRule.squirrels();

  @Rule
  public NeoTmpRepoRule neoRepo = new NeoTmpRepoRule();

  private ClbConfiguration config(){
    // use default prod API
    Properties props = dbSetup.getProperties();
    ClbConfiguration clb = new ClbConfiguration();
    clb.databaseName = props.getProperty("checklistbank.db.dataSource.databaseName");
    clb.serverName = props.getProperty("checklistbank.db.dataSource.serverName");
    clb.user = props.getProperty("checklistbank.db.dataSource.user");
    clb.password = props.getProperty("checklistbank.db.dataSource.password");
    return clb;
  }

  @Test
  public void testUsages() throws Exception {
    try (ClbSource src = new ClbSource(config(), neoRepo.cfg, UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4"), "squirrels")) {
      src.ignoreRanksAbove = Rank.SPECIES;
      src.init(true, false);
      int counter = 0;
      for (SrcUsage u : src) {
        counter++;
        System.out.print(u.key + "  ");
        System.out.println(u.scientificName);
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
      for (SrcUsage u : src) {
        counter++;
        System.out.print(u.key + "  ");
        System.out.println(u.scientificName);
      }
      assertEquals(12, counter);
    }
  }
}