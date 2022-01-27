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
package org.gbif.checklistbank.index.config;

import org.gbif.common.search.solr.SolrConfig;
import org.gbif.common.search.solr.SolrServerType;

import com.google.common.base.Strings;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
    return solrConfigProperties().buildSolr();
  }

  public static boolean empty(SolrConfig cfg) {
    return cfg.getServerType() == null
           || Strings.isNullOrEmpty(cfg.getServerHome())
           || (cfg.getServerType() == SolrServerType.HTTP && !cfg.getServerHome().startsWith("http"));
  }

}
