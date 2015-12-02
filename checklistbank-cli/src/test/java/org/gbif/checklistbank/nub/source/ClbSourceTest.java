package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;

import java.util.Properties;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by markus on 29/09/15.
 */
public class ClbSourceTest {
    @Rule
    public ClbDbTestRule dbSetup = ClbDbTestRule.squirrels();

    @Test
    public void testUsages() throws Exception {
        // use default prod API
        Properties props = dbSetup.getProperties();
        ClbConfiguration clb = new ClbConfiguration();
        clb.databaseName = props.getProperty("checklistbank.db.dataSource.databaseName");
        clb.serverName = props.getProperty("checklistbank.db.dataSource.serverName");
        clb.user = props.getProperty("checklistbank.db.dataSource.user");
        clb.password = props.getProperty("checklistbank.db.dataSource.password");

        ClbSource src = new ClbSource(clb, UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4"), "squirrels");
        src.ignoreRanksAbove = Rank.SPECIES;
        src.init(false);
        int counter = 0;
        for (SrcUsage u : src) {
            counter++;
            System.out.print(u.key + "  ");
            System.out.println(u.scientificName);
        }
        assertEquals(44, counter);

    }
}