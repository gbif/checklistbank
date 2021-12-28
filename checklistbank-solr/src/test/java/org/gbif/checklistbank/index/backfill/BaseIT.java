package org.gbif.checklistbank.index.backfill;

import org.gbif.checklistbank.index.guice.SpringSolrConfig;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule2;
import org.gbif.checklistbank.service.mybatis.service.MyBatisServiceITBase;
import org.gbif.checklistbank.service.mybatis.service.SpringServiceConfig;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.ConnectionInfo;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.embedded.PreparedDbProvider;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = BaseIT.ChecklistBankServiceTestConfiguration.class)
@ContextConfiguration(initializers = {BaseIT.ContextInitializer.class})
@ActiveProfiles("test")
public class BaseIT {

  protected DataSource dataSource;

  public BaseIT(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @TestConfiguration
  @PropertySource("classpath:application-test.yml")
  @Import(SpringSolrConfig.class)
  @SpringBootApplication(exclude = {RabbitAutoConfiguration.class})
  public static class ChecklistBankServiceTestConfiguration {
    public static void main(String[] args) {
      SpringApplication.run(MyBatisServiceITBase.ChecklistBankServiceTestConfiguration.class, args);
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
