package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.checklistbank.service.UsageService;

import java.util.List;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

public class UsageServiceMyBatisTest extends MyBatisServiceITBase {

  private final UsageService service;

  @Autowired
  public UsageServiceMyBatisTest(DataSource dataSource, UsageService usageService) {
    super(dataSource);
    this.service = usageService;
  }

  @Test
  public void testlistAll() {
    List<Integer> squirrels = service.listAll();
    assertEquals(46, squirrels.size());
  }

  @Test
  public void testlistParents() {
    List<Integer> squirrels = service.listParents(100000007);
    assertEquals(8, squirrels.size());
  }
}
