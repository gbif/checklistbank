package org.gbif.checklistbank.service.mybatis;

import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UsageServiceMyBatisTest {

  @Rule
  public DatabaseDrivenChecklistBankTestRule<UsageService> ddt =
    new DatabaseDrivenChecklistBankTestRule<UsageService>(UsageService.class);

  @Test
  public void testlistAll() {
    List<Integer> squirrels = ddt.getService().listAll();
    assertEquals(46, squirrels.size());
  }

  @Test
  public void testlistParents() {
    List<Integer> squirrels = ddt.getService().listParents(100000007);
    assertEquals(8, squirrels.size());
  }
}