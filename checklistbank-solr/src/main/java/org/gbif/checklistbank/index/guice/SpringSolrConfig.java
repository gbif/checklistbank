package org.gbif.checklistbank.index.guice;

import org.gbif.checklistbank.service.mybatis.service.SpringServiceConfig;
import org.gbif.common.search.solr.SolrConfig;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(SpringServiceConfig.class)
@ComponentScan(basePackages = "org.gbif.checklistbank.index")
public class SpringSolrConfig {

  SpringSolrConfig() {
    // TODO: move it to where it's used?
    System.setProperty("solr.lock.type", "native");
  }

  private static final int SYNC_THREADS = 2;

  @Bean("syncThreads")
  public Integer syncThreads() {
    return SYNC_THREADS;
  }

  @Bean
  @ConfigurationProperties("checklistbank.search.solr")
  public SolrConfig solrConfigProperties() {
    return new SolrConfig();
  }

  @Bean
  public SolrClient solrClient(SolrConfig solrConfig) {
    return solrConfig.buildSolr();
  }

  @Bean
  public EmbeddedSolrReference embeddedSolrReference(SolrClient solrClient) {
    return new EmbeddedSolrReference(solrClient);
  }
}
