package org.gbif.checklistbank.nub;

import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.neo.UsageDao;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

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
}