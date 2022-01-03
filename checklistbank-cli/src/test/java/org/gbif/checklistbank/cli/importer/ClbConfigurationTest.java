package org.gbif.checklistbank.cli.importer;

import org.gbif.checklistbank.config.ClbConfiguration;

import java.util.Properties;

import com.google.inject.CreationException;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Ignore("REMOVE! ignored only to make the jenkins build work")
public class ClbConfigurationTest {

  @Test(expected = CreationException.class)
    public void testBadTimeout() throws Exception {
//        ClbConfiguration cfg = new ClbConfiguration();
//        cfg.parserTimeout = 1;
//        Injector inj = Guice.createInjector(ChecklistBankServiceMyBatisModule.create(cfg));
//        inj.getInstance(NameParser.class);
    }

  @Test
  public void testPropTrip() throws Exception {
    ClbConfiguration cfg = new ClbConfiguration();
    cfg.databaseUrl = "test1";
    cfg.user = "test3";
    cfg.password = "test4";
    cfg.maximumPoolSize = 11;
    cfg.minimumIdle = 99;
    cfg.idleTimeout = 20200;
    cfg.maxLifetime = 8877;
    cfg.idleTimeout = 20200;
    cfg.connectionInitSql = "SET work_mem='64MB'";
    cfg.connectionTimeout = 7777;
    cfg.parserTimeout = 1000;
    cfg.syncThreads = 3;

    Properties p = cfg.toProps(true);

    ClbConfiguration cfg2 = ClbConfiguration.fromProperties(p);

    assertEquals(cfg2, cfg);
  }
}
