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
package org.gbif.checklistbank.service.mybatis.persistence.liquibase;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

/**
 * Class that updates the db schema using liquibase with a supplied jdbc connection.
 * It can be used from a (shaded) jar without maven and source files.
 */
public class DbSchemaUpdater {
  private static final Logger LOG = LoggerFactory.getLogger(DbSchemaUpdater.class);

  public static void update(Connection con) {
    LOG.info("Updating database schema with liquibase changesets ...");
    try {
      JdbcConnection jdbcConnection = new JdbcConnection(con);
      liquibase.database.Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);

      //if (database instanceof PostgresDatabase) {
      //  database = new PostgresDatabase() {
      //    @Override
      //    public String escapeObjectName(String objectName, Class<? extends DatabaseObject> objectType) {
      //      return objectName;
      //    }
      //  };
      //  database.setConnection(jdbcConnection);
      //}

      ResourceAccessor accessor = new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader());
      Liquibase liq = new Liquibase("liquibase/checklistbank/master.xml", accessor, database);
      liq.update("prod");
      LOG.info("Database schema updated");

    } catch (Exception e) {
      LOG.error("Database liquibase update failed", e);
      throw new RuntimeException(e);
    }
  }
}
