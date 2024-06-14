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
package org.gbif.checklistbank.cli.common;

import org.gbif.api.ws.mixin.Mixins;
import org.gbif.checklistbank.cli.admin.AdminConfiguration;
import org.gbif.checklistbank.cli.config.ElasticsearchConfiguration;
import org.gbif.checklistbank.cli.stubs.MessagePublisherStub;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.index.NameUsageIndexServiceEs;
import org.gbif.checklistbank.index.model.NameUsageAvro;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.mybatis.export.Exporter;
import org.gbif.checklistbank.service.mybatis.persistence.ChecklistBankMyBatisConfiguration;
import org.gbif.checklistbank.utils.NameParsers;
import org.gbif.common.messaging.ConnectionParameters;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.DefaultMessageRegistry;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.common.search.es.EsClient;
import org.gbif.registry.ws.client.*;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.MapPropertySource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

/** Utility class to create Spring contexts to be used later in CLI applications. */
public class SpringContextBuilder {
  public static String SEARCH_INDEX_SERVICE_BEAN_NAME = "searchIndexService";
  public static String EXPORT_REPOSITORY_BEAN_NAME = "exportRepository";

  private String[] basePackages;

  private Class<?>[] componentClasses;

  private ClbConfiguration clbConfiguration;

  private MessagingConfiguration messagingConfiguration;

  private ElasticsearchConfiguration elasticsearchConfiguration;

  private AdminConfiguration adminConfiguration;


  private SpringContextBuilder() {}

  public static SpringContextBuilder create() {
    return new SpringContextBuilder();
  }

  public SpringContextBuilder withClbConfiguration(ClbConfiguration clbConfiguration) {
    this.clbConfiguration = clbConfiguration;
    return this;
  }

  public SpringContextBuilder withMessagingConfiguration(
      MessagingConfiguration messagingConfiguration) {
    this.messagingConfiguration = messagingConfiguration;
    return this;
  }

  public SpringContextBuilder withElasticsearchConfiguration(ElasticsearchConfiguration elasticsearchConfiguration) {
    this.elasticsearchConfiguration = elasticsearchConfiguration;
    return this;
  }

  public SpringContextBuilder withAdminConfiguration(AdminConfiguration adminConfiguration) {
    this.adminConfiguration = adminConfiguration;
    return this;
  }

  public SpringContextBuilder withComponents(Class<?>... componentClasses) {
    this.componentClasses = componentClasses;
    return this;
  }

  public SpringContextBuilder withScanPackages(String... basePackages) {
    this.basePackages = basePackages;
    return this;
  }

  public AnnotationConfigApplicationContext build() {

    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

    ctx.registerBean("clbObjectMapper", ObjectMapper.class, this::clbObjectMapper);

    Set<String> packages =
        basePackages == null ? new HashSet<>() : new HashSet<>(Arrays.asList(basePackages));

    //ignore error: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'compositeCompatibilityVerifier'
    ((DefaultListableBeanFactory) ctx.getBeanFactory()).destroySingleton("org.springframework.cloud.configuration.CompatibilityVerifierAutoConfiguration");
    if (clbConfiguration != null) {
      ctx.register(ApplicationConfig.class);
      ctx.register(ChecklistBankMyBatisConfiguration.class);
      ctx.registerBean(ClbConfiguration.class, () -> clbConfiguration);
      ctx.register(MybatisAutoConfiguration.class);
      NameParsers.INSTANCE.setTimeout(clbConfiguration.parserTimeout);

      ctx.getEnvironment()
          .getPropertySources()
          .addLast(
              new MapPropertySource(
                  "clbConfigProperties",
                  Map.of(
                      "checklistbank.nub.importThreads", clbConfiguration.syncThreads,
                      "checklistbank.parser.timeout", clbConfiguration.parserTimeout
                  )
              ));
    }

    if (adminConfiguration != null) {
      ctx.registerBean(EXPORT_REPOSITORY_BEAN_NAME, File.class, () -> adminConfiguration.exportRepository);
      ctx.registerBean(Exporter.class);

      if (adminConfiguration.registry != null) {
        ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder.withUrl(adminConfiguration.registry.wsUrl);
        clientBuilder.withObjectMapper(
            JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport()
        );
        if (adminConfiguration.registry.password != null && adminConfiguration.registry.user != null) {
          clientBuilder.withCredentials(adminConfiguration.registry.user, adminConfiguration.registry.password);
        }
        ctx.registerBean(DatasetClient.class, () -> clientBuilder.build(DatasetClient.class));
        ctx.registerBean(OrganizationClient.class, () -> clientBuilder.build(OrganizationClient.class));
        ctx.registerBean(InstallationClient.class, () -> clientBuilder.build(InstallationClient.class));
        ctx.registerBean(NetworkClient.class, () -> clientBuilder.build(NetworkClient.class));
        ctx.registerBean(NodeClient.class, () -> clientBuilder.build(NodeClient.class));
      }
    }

    if (messagingConfiguration != null) {
      if (messagingConfiguration.host != null) {
        ctx.registerBean(
            "messagePublisher",
            MessagePublisher.class,
            () -> {
              try {
                return new DefaultMessagePublisher(
                    new ConnectionParameters(
                        messagingConfiguration.host,
                        messagingConfiguration.port,
                        messagingConfiguration.username,
                        messagingConfiguration.password,
                        messagingConfiguration.virtualHost),
                    new DefaultMessageRegistry(),
                    clbObjectMapper());
              } catch (IOException e) {
                  throw new RuntimeException(e);
              }
            });
      } else {
        ctx.registerBean("messagePublisher", MessagePublisher.class, MessagePublisherStub::new);
      }
    }

    if (elasticsearchConfiguration != null) {
      if (elasticsearchConfiguration.enabled) {

        EsClient.EsClientConfiguration esClientConfiguration = new EsClient.EsClientConfiguration();
        esClientConfiguration.setHosts(elasticsearchConfiguration.hosts);
        esClientConfiguration.setConnectionTimeOut(elasticsearchConfiguration.connectionTimeOut);
        esClientConfiguration.setConnectionRequestTimeOut(elasticsearchConfiguration.connectionRequestTimeOut);
        esClientConfiguration.setSocketTimeOut(elasticsearchConfiguration.socketTimeOut);
        ctx.registerBean(EsClient.EsClientConfiguration.class, () -> esClientConfiguration);

        final ElasticsearchClient client = EsClient.provideEsClient(esClientConfiguration);
        // we know its using JacksonJsonpMapper
        JacksonJsonpMapper jjm = (JacksonJsonpMapper) client._jsonpMapper();
        jjm.objectMapper().addMixIn(NameUsageAvro.class, AvroMixin.class);

        ctx.registerBean(ElasticsearchClient.class, () -> client);
        ctx.registerBean(EsClient.class, () -> new EsClient(client));

        ctx.registerBean(SEARCH_INDEX_SERVICE_BEAN_NAME, NameUsageIndexServiceEs.class);
        ctx.registerBean("syncThreads", Integer.class, elasticsearchConfiguration.syncThreads);
        ctx.registerBean("indexName", String.class, elasticsearchConfiguration.alias);

      } else {
        ctx.registerBean(SEARCH_INDEX_SERVICE_BEAN_NAME, DatasetImportService.class, DatasetImportService::passThru);
      }
    }

    ctx.getEnvironment()
        .getPropertySources()
        .addLast(
            new MapPropertySource(
                "ManagedProperties",
                Map.of("spring.cloud.compatibility-verifier.enabled", "false")
            ));

    if (!packages.isEmpty()) {
      ctx.scan(packages.toArray(new String[] {}));
    }

    if (componentClasses != null) {
      for (Class<?> c : componentClasses) {
        ctx.register(c);
      }
    }

    ctx.refresh();
    ctx.start();
    return ctx;
  }

  public ObjectMapper clbObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    // determines whether encountering of unknown properties (ones that do not map to a property,
    // and there is no "any setter" or handler that can handle it) should result in a failure
    // (throwing a JsonMappingException) or not.
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Enforce use of ISO-8601 format dates (http://wiki.fasterxml.com/JacksonFAQDateHandling)
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    Mixins.getPredefinedMixins().forEach(objectMapper::addMixIn);

    return objectMapper;
  }

  /** Class to help with the loading and injection of */
  @SpringBootApplication(
      exclude = {
        DataSourceAutoConfiguration.class,
        LiquibaseAutoConfiguration.class,
        FreeMarkerAutoConfiguration.class,
        RabbitAutoConfiguration.class
      })
  @MapperScan("org.gbif.checklistbank.service.mybatis.persistence.mapper")
  @EnableConfigurationProperties
  @ComponentScan
  static class ApplicationConfig {

  }

  abstract static class AvroMixin {
    @JsonIgnore
    public abstract org.apache.avro.Schema getSchema();
  }
}
