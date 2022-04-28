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
package org.gbif.checklistbank.ws;

import org.gbif.checklistbank.service.mybatis.service.SpringServiceConfig;
import org.gbif.checklistbank.ws.config.SpringElasticsearchConfiguration;
import org.gbif.ws.remoteauth.RemoteAuthClient;
import org.gbif.ws.remoteauth.RemoteAuthWebSecurityConfigurer;
import org.gbif.ws.remoteauth.RestTemplateRemoteAuthClient;
import org.gbif.ws.security.AppKeySigningService;
import org.gbif.ws.security.FileSystemKeyStore;
import org.gbif.ws.security.GbifAuthServiceImpl;
import org.gbif.ws.security.GbifAuthenticationManagerImpl;
import org.gbif.ws.server.filter.AppIdentityFilter;
import org.gbif.ws.server.filter.IdentityFilter;
import org.gbif.ws.server.mapper.WebApplicationExceptionMapper;

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
@Import({SpringServiceConfig.class, SpringElasticsearchConfiguration.class})
@EnableConfigurationProperties
@ComponentScan(
  basePackages = {
    "org.gbif.ws.server.interceptor",
    "org.gbif.ws.server.aspect",
    "org.gbif.ws.server.filter",
    "org.gbif.ws.server.advice",
    "org.gbif.ws.server.mapper",
    "org.gbif.checklistbank.index.service", //Search service
    "org.gbif.checklistbank.config", //Metrics
    "org.gbif.checklistbank.ws"
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
        GbifAuthServiceImpl.class,
        WebApplicationExceptionMapper.class
      })
  })

public class ChecklistBankWsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ChecklistBankWsApplication.class, args);
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
