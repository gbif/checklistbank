package org.gbif.checklistbank.ws;

import org.gbif.ws.remoteauth.RemoteAuthClient;
import org.gbif.ws.remoteauth.RemoteAuthWebSecurityConfigurer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticSearchRestHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication(
  exclude = {
    ElasticSearchRestHealthContributorAutoConfiguration.class,
    RabbitAutoConfiguration.class
  })
@EnableConfigurationProperties
@ComponentScan(
  basePackages = {
    "org.gbif.ws.server.interceptor",
    "org.gbif.ws.server.aspect",
    "org.gbif.ws.server.filter",
    "org.gbif.ws.server.advice",
    "org.gbif.ws.server.mapper",
    "org.gbif.checklistbank.ws"
  })

/**
 * TODO: Old Guice Modules to include
 *  modules.add(new MetricModule(properties));
 *     modules.add(new ChecklistBankServiceMyBatisModule(properties));
 *     modules.add(new SearchModule(properties, true));
 * */
public class ChecklistBankWsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ChecklistBankWsApplication.class, args);
  }

  @Configuration
  public static class SecurityConfiguration extends RemoteAuthWebSecurityConfigurer {

    public SecurityConfiguration(ApplicationContext context, RemoteAuthClient remoteAuthClient) {
      super(context, remoteAuthClient);
    }
  }
}
