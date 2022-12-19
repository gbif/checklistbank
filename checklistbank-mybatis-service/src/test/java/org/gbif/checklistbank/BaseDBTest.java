package org.gbif.checklistbank;

import java.sql.SQLException;
import java.time.Duration;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class BaseDBTest {

  public static final PostgreSQLContainer PG_CONTAINER;

  static {
    PG_CONTAINER = new PostgreSQLContainer("postgres:11.1").withDatabaseName("clb");
    PG_CONTAINER.withReuse(true).withLabel("reuse.tag", "clb_ITs_PG_container");
    PG_CONTAINER.setWaitStrategy(
        Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)));
    PG_CONTAINER.start();

    try {
      updateLiquibase();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void updateLiquibase() throws SQLException, LiquibaseException {
    Database databaseLiquibase;
    databaseLiquibase =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(
                new JdbcConnection(PG_CONTAINER.createConnection("")));
    Liquibase liquibase =
        new Liquibase(
            "liquibase/checklistbank/master.xml",
            new ClassLoaderResourceAccessor(),
            databaseLiquibase);
    liquibase.update(new Contexts());
  }
}
