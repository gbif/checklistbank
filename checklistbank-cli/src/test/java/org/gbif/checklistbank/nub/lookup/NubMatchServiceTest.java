package org.gbif.checklistbank.nub.lookup;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.io.FileUtils;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.Mybatis;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

/**
 *
 */
@Ignore("A manual test class to rematch individual datasets")
public class NubMatchServiceTest {

  @Test
  public void matchDataset() throws Exception {
    ClbConfiguration clb = new ClbConfiguration();
    clb.serverName = "backbonebuild-vh.gbif.org";
    clb.databaseName = "clb";
    clb.user = "clb";
    clb.password = "";

    NeoConfiguration neo = new NeoConfiguration();
    neo.neoRepository = new File(FileUtils.getTempDirectory(), "clb");

    Injector inj = Guice.createInjector(ChecklistBankServiceMyBatisModule.create(clb));
    DatasetImportService sqlService = inj.getInstance(Key.get(DatasetImportService.class, Mybatis.class));

    IdLookup lookup = IdLookupImpl.temp().load(clb, false);
    NubMatchService service = new NubMatchService(clb, neo, lookup, sqlService, null);
    // IUCN 19491596-35ae-4a91-9a98-85cf505f1bd3
    service.matchDataset(UUID.fromString("19491596-35ae-4a91-9a98-85cf505f1bd3"));
  }

}