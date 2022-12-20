package org.gbif.checklistbank;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.function.Supplier;

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
    PG_CONTAINER = createPostgreSQLContainer();
    PG_CONTAINER.start();

    try {
      updateLiquibase(PG_CONTAINER);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static PostgreSQLContainer createPostgreSQLContainer() {
    PostgreSQLContainer container =
        new PostgreSQLContainer("postgres:11.1").withDatabaseName("clb");
    container.withReuse(true).withLabel("reuse.tag", "clb_ITs_PG_container");
    container.setWaitStrategy(
        Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)));
    return container;
  }

  public static void updateLiquibase(PostgreSQLContainer pgContainer)
      throws SQLException, LiquibaseException {
    Database databaseLiquibase;
    databaseLiquibase =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(
                new JdbcConnection(pgContainer.createConnection("")));
    Liquibase liquibase =
        new Liquibase(
            "liquibase/checklistbank/master.xml",
            new ClassLoaderResourceAccessor(),
            databaseLiquibase);
    liquibase.update(new Contexts());
  }

  protected Supplier<Connection> createConnectionSupplier() {
    return createConnectionSupplier(PG_CONTAINER);
  }

  public static Supplier<Connection> createConnectionSupplier(
      PostgreSQLContainer postgreSQLContainer) {
    return () -> {
      try {
        return postgreSQLContainer.createConnection("");
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    };
  }
}
