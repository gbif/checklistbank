package org.gbif.checklistbank.nub;

import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.validation.NubAssertions;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.utils.ClbConfigurationUtils;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class NubAssertionsTest {

  @Test
  public void testValidate() throws Exception {
    UsageDao dao = UsageDao.temporaryDao(10);
    NubDb db = NubDb.create(dao, AuthorComparator.createWithoutAuthormap());
    NubAssertions ass = new NubAssertions(db);
    assertFalse(ass.validate());
  }


  @Test
  @Ignore("manual test only")
  public void validatePostgresNub() throws Exception {
    ClbConfiguration cfg = ClbConfigurationUtils.uat();
    Injector inj = Guice.createInjector(ChecklistBankServiceMyBatisModule.create(cfg));

    NubAssertions ass = new NubAssertions(inj.getInstance(NameUsageService.class));

    assertTrue(ass.validate());
  }
}