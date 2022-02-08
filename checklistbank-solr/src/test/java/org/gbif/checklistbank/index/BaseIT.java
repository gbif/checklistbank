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
package org.gbif.checklistbank.index;

import org.gbif.ChecklistbankMyBatisServiceITBase;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.index.backfill.IndexingConfigKeys;
import org.gbif.checklistbank.index.backfill.SolrBackfill;
import org.gbif.checklistbank.index.config.SpringSolrConfig;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.service.SpringServiceConfig;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.zonky.test.db.postgres.embedded.ConnectionInfo;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.embedded.PreparedDbProvider;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
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

  @TestConfiguration
  @PropertySource("classpath:application-test.yml")
  @Import({SpringSolrConfig.class, SpringServiceConfig.class})
  @SpringBootApplication(
      scanBasePackages = {
        "org.gbif.checklistbank.index.config",
        "org.gbif.checklistbank.index.service"
      },
      exclude = {RabbitAutoConfiguration.class})
  public static class ChecklistBankServiceTestConfiguration {

    @Bean
    public SolrBackfill solrBackfill(
      SolrClient solr,
      @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.THREADS + "}") Integer threads,
      @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.BATCH_SIZE + "}") Integer batchSize,
      @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.LOG_INTERVAL + "}")Integer logInterval,
      UsageService nameUsageService,
      VernacularNameService vernacularNameService,
      DescriptionService descriptionService,
      DistributionService distributionService,
      SpeciesProfileService speciesProfileService) {
      return new SolrBackfill(solr,
                              threads,
                              batchSize,
                              logInterval,
                              nameUsageService,
                              new NameUsageDocConverter(),
                              vernacularNameService,
                              descriptionService,
                              distributionService,
                              speciesProfileService);
    }

    public static void main(String[] args) {
      SpringApplication.run(ChecklistbankMyBatisServiceITBase.ChecklistBankServiceTestConfiguration.class, args);
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
          LiquibasePreparer.forClasspathLocation("liquibase/checklistbank/master.xml");
        PreparedDbProvider provider =
          PreparedDbProvider.forPreparer(liquibasePreparer, builderCustomizers);
        ConnectionInfo connectionInfo = provider.createNewDatabase();
        TestPropertyValues.of(Stream.of(testPropertyPairs(connectionInfo)).toArray(String[]::new))
          .applyTo(configurableApplicationContext.getEnvironment());

      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    /** Creates the datasource and Solr settings from the embedded database. */
    String[] testPropertyPairs(ConnectionInfo connectionInfo) {
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
        "checklistbank.datasource.hikari.connectionInitSql=SET work_mem='64MB'",
        "checklistbank.search.solr.serverType=EMBEDDED",
        "checklistbank.search.solr.collection=checklistbank",
        "checklistbank.search.solr.serverHome=",
        "checklistbank.search.solr.deleteOnExit=true",
        "checklistbank.indexer.threads=2",
        "checklistbank.indexer.batchSize=5",
        "checklistbank.indexer.writers=1",
        "checklistbank.indexer.logInterval=60"
      };
    }
  }
}
