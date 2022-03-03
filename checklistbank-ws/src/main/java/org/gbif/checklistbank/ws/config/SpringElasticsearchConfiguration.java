package org.gbif.checklistbank.ws.config;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.checklistbank.search.service.NameUsageSearchServiceEs;
import org.gbif.common.search.es.EsClient;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringElasticsearchConfiguration {

  @ConfigurationProperties(prefix = "checklistbank.elasticsarch")
  @Bean
  public EsClient.EsClientConfiguration esClientConfiguration() {
    return new EsClient.EsClientConfiguration();
  }

  @Bean
  public RestHighLevelClient restHighLevelClient(EsClient.EsClientConfiguration esClientConfiguration) {
    return EsClient.provideEsClient(esClientConfiguration);
  }

  @Bean
  public NameUsageSearchService nameUsageSearchService(RestHighLevelClient client, @Value("${checklistbank.elasticsarch.index}") String index) {
    return new NameUsageSearchServiceEs(index, client);
  }
}
