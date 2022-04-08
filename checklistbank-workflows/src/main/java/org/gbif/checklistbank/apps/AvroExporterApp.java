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
package org.gbif.checklistbank.apps;

import org.gbif.checklistbank.exporter.AvroExporter;
import org.gbif.checklistbank.index.OccurrenceCountClient;
import org.gbif.checklistbank.service.mybatis.service.SpringServiceConfig;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import java.time.Duration;

import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Checklist Bank multi-threaded name usage indexer. This class creates a pool of configurable
 * <i>threads</i> that concurrently execute a number of jobs each processing a configurable number
 * of name usages (<i>batchSize</i>) using a configurable number of concurrent lucene
 * <i>writers</i>. The indexer makes direct use of the mybatis layer and requires a checklist bank
 * datasource to be configured.
 */
@SpringBootApplication(
    exclude = {
      DataSourceAutoConfiguration.class,
      LiquibaseAutoConfiguration.class,
      FreeMarkerAutoConfiguration.class,
      ArchaiusAutoConfiguration.class,
      RabbitAutoConfiguration.class,
      GsonAutoConfiguration.class,
      FeignAutoConfiguration.class
    })
@Profile("!test")
@Component
@Import({AvroExporterApp.AvroExporterConfig.class, SpringServiceConfig.class, AvroExporter.class, MybatisAutoConfiguration.class})
@ComponentScan(
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE)
    })
public class AvroExporterApp implements CommandLineRunner {

  private AvroExporter avroExporter;

  @Autowired
  public AvroExporterApp(AvroExporter avroExporter) {
    this.avroExporter = avroExporter;
  }

  private static final Logger LOG = LoggerFactory.getLogger(AvroExporterApp.class);

  /** Entry point for execution. Commandline arguments are: 0: required path to property file */
  public static void main(String[] args) throws Exception {
    SpringApplication app = new SpringApplication(AvroExporterApp.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    app.setBannerMode(Banner.Mode.OFF);
    app.run(args);
  }

  @Override
  public void run(String... args) throws Exception {
    if (args.length == 0) {
      throw new IllegalArgumentException("Path to property file required");
    }
    avroExporter.run();
    LOG.info("Indexing done. Time to exit.");
  }

  @Configuration
  public static class AvroExporterConfig {

    @Bean
    public OccurrenceCountClient occurrenceCountClient(@Value("${apiUrl}") String apiUrl) {
      return OccurrenceCountClient.cachingClient(new ClientBuilder()
                                                  .withUrl(apiUrl)
                                                  .withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport())
                                                  .withExponentialBackoffRetry(Duration.ofMillis(100), 1.5, 3)
                                                  .build(OccurrenceCountClient.class));
    }
  }
}
