package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Simply checks no exceptions occurr when loading the standard squirrelts dataset. */
public class DbLoaderTest extends PostgresITBase {

  @Autowired
  public DbLoaderTest(DataSource dataSource) {
    super(dataSource);
  }

  @Test
  public void testLoad() throws Exception {
    DbLoader.load(dataSource.getConnection(), "squirrels", true);
  }
}
