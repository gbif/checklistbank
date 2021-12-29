package org.gbif.checklistbank.ws.nub;

import org.gbif.checklistbank.service.mybatis.service.SpringServiceConfig;
import org.gbif.ws.remoteauth.RemoteAuthClient;
import org.gbif.ws.remoteauth.RemoteAuthWebSecurityConfigurer;
import org.gbif.ws.remoteauth.RestTemplateRemoteAuthClient;
import org.gbif.ws.security.AppKeySigningService;
import org.gbif.ws.security.FileSystemKeyStore;
import org.gbif.ws.security.GbifAuthServiceImpl;
import org.gbif.ws.security.GbifAuthenticationManagerImpl;
import org.gbif.ws.server.filter.AppIdentityFilter;
import org.gbif.ws.server.filter.IdentityFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticSearchRestHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@SpringBootApplication(
    exclude = {
      ElasticSearchRestHealthContributorAutoConfiguration.class,
      RabbitAutoConfiguration.class
    })
@Import(SpringServiceConfig.class)
@EnableConfigurationProperties
@ComponentScan(
    basePackages = {
      "org.gbif.ws.server.interceptor",
      "org.gbif.ws.server.aspect",
      "org.gbif.ws.server.filter",
      "org.gbif.ws.server.advice",
      "org.gbif.ws.server.mapper",
      "org.gbif.nub",
      "org.gbif.checklistbank.ws.nub"
    },
  excludeFilters = {
    @ComponentScan.Filter(
      type = FilterType.ASSIGNABLE_TYPE,
      classes = {
        AppKeySigningService.class,
        FileSystemKeyStore.class,
        IdentityFilter.class,
        AppIdentityFilter.class,
        GbifAuthenticationManagerImpl.class,
        GbifAuthServiceImpl.class
      })
  })
public class NubWsApplication {
  public static void main(String[] args) {
    SpringApplication.run(NubWsApplication.class, args);
  }

  @Bean
  public RemoteAuthClient remoteAuthClient(
    RestTemplateBuilder builder, @Value("${gbif.api.url}") String gbifApiUrl) {
    return RestTemplateRemoteAuthClient.createInstance(builder, gbifApiUrl);
  }

  @Configuration
  public static class SecurityConfiguration extends RemoteAuthWebSecurityConfigurer {

    public SecurityConfiguration(ApplicationContext context, RemoteAuthClient remoteAuthClient) {
      super(context, remoteAuthClient);
    }

  }
}
