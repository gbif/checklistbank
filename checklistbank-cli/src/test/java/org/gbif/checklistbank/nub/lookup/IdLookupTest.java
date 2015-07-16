package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;

public class IdLookupTest {

    @Rule
    public DatabaseDrivenChecklistBankTestRule<NameUsageService> ddt =
            new DatabaseDrivenChecklistBankTestRule<NameUsageService>(NameUsageService.class);

    @Test
    public void testLookup() throws IOException, SQLException {
        ClbConfiguration cfg = new ClbConfiguration();

        Properties props = ddt.getProperties();
        cfg.databaseName = props.getProperty("checklistbank.db.dataSource.databaseName");
        cfg.serverName = props.getProperty("checklistbank.db.dataSource.serverName");
        cfg.user = props.getProperty("checklistbank.db.dataSource.user");
        cfg.password = props.getProperty("checklistbank.db.dataSource.password");

        IdLookup l = new IdLookup(cfg);
    }
}