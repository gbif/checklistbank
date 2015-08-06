package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IdLookupIT {

    @Rule
    public ClbDbTestRule dbunit = ClbDbTestRule.squirrels();

    @Test
    public void testLookup() throws IOException, SQLException {
        ClbConfiguration cfg = new ClbConfiguration();

        Properties props = dbunit.getProperties();
        cfg.databaseName = props.getProperty("checklistbank.db.dataSource.databaseName");
        cfg.serverName = props.getProperty("checklistbank.db.dataSource.serverName");
        cfg.user = props.getProperty("checklistbank.db.dataSource.user");
        cfg.password = props.getProperty("checklistbank.db.dataSource.password");

        IdLookup l = new IdLookup(cfg);
        assertEquals(2, l.size());
        assertEquals(1, l.match("Animalia", Rank.KINGDOM, Kingdom.ANIMALIA).getKey());

        assertEquals(10, l.match("Rodentia", Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertNull(l.match("Rodentia", Rank.FAMILY, Kingdom.ANIMALIA));
        assertNull(l.match("Rodentia", Rank.ORDER, Kingdom.PLANTAE));
        assertNull(l.match("Rodenti", Rank.ORDER, Kingdom.ANIMALIA));

        assertEquals(10, l.match("Rodentia", "Bowdich", "1821", Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertEquals(10, l.match("Rodentia", "Bowdich", "1221", Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertEquals(10, l.match("Rodentia", "Bowdich", null, Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertEquals(10, l.match("Rodentia", null, "1821", Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertEquals(10, l.match("Rodentia", "Bow.", null, Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertEquals(10, l.match("Rodentia", "Bow", "1821", Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertEquals(10, l.match("Rodentia", "B", "1821", Rank.ORDER, Kingdom.ANIMALIA).getKey());
        assertNull(l.match("Rodentia", "Mill.", "1823", Rank.ORDER, Kingdom.ANIMALIA));
    }
}