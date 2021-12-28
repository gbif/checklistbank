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
package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule2;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.zonky.test.db.postgres.embedded.ConnectionInfo;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.embedded.PreparedDbProvider;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = MyBatisServiceITBase.ChecklistBankServiceTestConfiguration.class)
@ContextConfiguration(initializers = {MyBatisServiceITBase.ContextInitializer.class})
@ActiveProfiles("test")
public class MyBatisServiceITBase {

  @RegisterExtension public ClbDbTestRule2 sbSetup;

  protected DataSource dataSource;

  public MyBatisServiceITBase(DataSource dataSource) {
    this.dataSource = dataSource;
    this.sbSetup = ClbDbTestRule2.squirrels(dataSource);
  }

  @TestConfiguration
  @PropertySource("classpath:application-test.yml")
  @Import(SpringServiceConfig.class) // actually not needed, it gets scanned by default
  @SpringBootApplication(exclude = {RabbitAutoConfiguration.class})
  public static class ChecklistBankServiceTestConfiguration {
    public static void main(String[] args) {
      SpringApplication.run(ChecklistBankServiceTestConfiguration.class, args);
    }
  }

  /** Custom ContextInitializer to expose the registry DB data source and search flags. */
  public static class ContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private final List<Consumer<EmbeddedPostgres.Builder>> builderCustomizers =
        new CopyOnWriteArrayList<>();

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      try {
        LiquibasePreparer liquibasePreparer =
            LiquibasePreparer.forClasspathLocation("liquibase/master.xml");
        PreparedDbProvider provider =
            PreparedDbProvider.forPreparer(liquibasePreparer, builderCustomizers);
        ConnectionInfo connectionInfo = provider.createNewDatabase();

        TestPropertyValues.of(Stream.of(dbTestPropertyPairs(connectionInfo)).toArray(String[]::new))
            .applyTo(configurableApplicationContext.getEnvironment());
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    /** Creates the registry datasource settings from the embedded database. */
    String[] dbTestPropertyPairs(ConnectionInfo connectionInfo) {
      return new String[] {
        "checklistbank.datasource.url=jdbc:postgresql://localhost:"
            + connectionInfo.getPort()
            + "/"
            + connectionInfo.getDbName(),
        "checklistbank.datasource.username=" + connectionInfo.getUser(),
        "checklistbank.datasource.password=",
        "checklistbank.datasource.hikari.maximumPoolSize=2",
        "checklistbank.datasource.hikari.minimumIdle=1",
        "checklistbank.datasource.hikari.idleTimeout=60000",
        "checklistbank.datasource.hikari.connectionTimeout=2000",
        "checklistbank.datasource.hikari.leakDetectionThreshold=10000",
        "checklistbank.datasource.hikari.connectionInitSql=SET work_mem='64MB'"
      };
    }
  }
}
