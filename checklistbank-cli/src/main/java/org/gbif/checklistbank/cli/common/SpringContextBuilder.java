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

import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.ws.mixin.Mixins;
import org.gbif.checklistbank.cli.stubs.MessagePublisherStub;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.persistence.ChecklistBankMyBatisConfiguration;
import org.gbif.common.messaging.ConnectionParameters;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.DefaultMessageRegistry;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.common.search.solr.SolrConfig;
import org.gbif.nameparser.NameParserGbifV1;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.solr.client.solrj.SolrClient;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/** Utility class to create Spring contexts to be used later in CLI applications. */
public class SpringContextBuilder {

  private String[] basePackages;

  private Class<?>[] componentClasses;

  private ClbConfiguration clbConfiguration;

  private MessagingConfiguration messagingConfiguration;

  private SolrConfig solrConfig;

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

  public SpringContextBuilder withSolrConfig(SolrConfig solrConfig) {
    this.solrConfig = solrConfig;
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

    if (clbConfiguration != null) {
      ctx.register(ApplicationConfig.class);
      ctx.register(ChecklistBankMyBatisConfiguration.class);
      ctx.registerBean(ClbConfiguration.class, () -> clbConfiguration);
      ctx.register(MybatisAutoConfiguration.class);
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

    if (solrConfig != null) {
      ctx.registerBean(SolrClient.class, () -> solrConfig.buildSolr());
    }

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

  // TODO: 03/01/2022 check implementation
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
        ArchaiusAutoConfiguration.class,
        SolrAutoConfiguration.class,
        RabbitAutoConfiguration.class
      })
  @MapperScan("org.gbif.checklistbank.service.mybatis.persistence.mapper")
  @EnableConfigurationProperties
  @ComponentScan
  static class ApplicationConfig {

    @Bean
    public NameParser nameParser(@Value("${parserTimeout:500}") long parserTimeout) {
      return new NameParserGbifV1(parserTimeout);
    }
  }
}
