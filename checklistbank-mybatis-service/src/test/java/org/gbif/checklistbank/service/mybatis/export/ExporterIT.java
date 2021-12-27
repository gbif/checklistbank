package org.gbif.checklistbank.service.mybatis.export;

import org.gbif.api.model.Constants;
import org.gbif.api.model.registry.Dataset;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule2;
import org.gbif.checklistbank.service.mybatis.service.SpringServiceConfig;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.ConnectionInfo;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.embedded.PreparedDbProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

/** Export squirrel test db as dwca */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ExporterIT.ChecklistBankServiceTestConfiguration.class)
@ContextConfiguration(initializers = {ExporterIT.ContextInitializer.class})
@ActiveProfiles("test")
public class ExporterIT {

  private String dbUserName;
  private String dbName;
  private String dbPassword;

  @RegisterExtension public ClbDbTestRule2 sbSetup;

  @Autowired
  public ExporterIT(
      DataSource dataSource,
      @Value("${checklistbank.datasource.username}") String dbUserName,
      @Value("${checklistbank.datasource.dbname}") String dbName,
      @Value("${checklistbank.datasource.password}") String dbPassword) {
    this.dbUserName = dbUserName;
    this.dbName = dbName;
    this.dbPassword = dbPassword;
    sbSetup = ClbDbTestRule2.squirrels(dataSource);
  }

  @Test
  public void testExport() throws Exception {
    ClbConfiguration cfg = new ClbConfiguration();
    cfg.serverName = "localhost";
    cfg.databaseName = dbName;
    cfg.user = dbUserName;
    cfg.password = dbPassword;

    File repository = FileUtils.createTempDir();

    try {
      Exporter exp = Exporter.create(repository, cfg, "http://api.gbif.org/v1");
      exp.export(dataset(Constants.NUB_DATASET_KEY));

      exp.export(dataset(ClbDbTestRule2.SQUIRRELS_DATASET_KEY));

    } finally {
      org.apache.commons.io.FileUtils.deleteDirectory(repository);
    }
  }

  private static Dataset dataset(UUID key) {
    Dataset d = new Dataset();
    d.setKey(key);
    d.setTitle("Dataset " + key);
    return d;
  }

  @TestConfiguration
  @PropertySource("classpath:application-test.yml")
  @Import(SpringServiceConfig.class)
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
        "checklistbank.datasource.dbname=" + connectionInfo.getDbName()
      };
    }
  }
}
