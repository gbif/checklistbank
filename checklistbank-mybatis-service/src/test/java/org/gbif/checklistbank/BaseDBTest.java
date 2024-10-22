/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.function.Supplier;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

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
