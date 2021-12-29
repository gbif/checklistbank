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
package org.gbif.checklistbank.index.guice;

import org.gbif.checklistbank.index.NameUsageIndexServicePassThru;
import org.gbif.checklistbank.index.NameUsageIndexServiceSolr;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.common.search.solr.SolrConfig;
import org.gbif.common.search.solr.SolrServerType;

import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.PrivateModule;
import com.google.inject.Scopes;

/**
 * Guice module that binds a DatasetImportService expecting the mybatis module to be available already.
 */
public class RealTimeModule extends PrivateModule {
  private static Logger LOG = LoggerFactory.getLogger(RealTimeModule.class);
  private final SolrConfig cfg;
  private final int syncThreads = 2;;

  public RealTimeModule(SolrConfig cfg) {
    this.cfg = cfg;
  }

  public static boolean empty(SolrConfig cfg) {
    return cfg.getServerType() == null
        || Strings.isNullOrEmpty(cfg.getServerHome())
        || (cfg.getServerType() == SolrServerType.HTTP && !cfg.getServerHome().startsWith("http"));
    }

  @Override
  protected void configure() {
    if (empty(cfg)) {
      bind(DatasetImportService.class)
          .annotatedWith(Solr.class)
          .to(NameUsageIndexServicePassThru.class)
          .in(Scopes.SINGLETON);
      LOG.info("No solr service configured. Using pass thru mock solr service.");

    } else {
      bind(SolrClient.class).toInstance(cfg.buildSolr());
      expose(SolrClient.class);
      bind(Integer.class)
          .annotatedWith(Solr.class)
          .toInstance(syncThreads);
      bind(DatasetImportService.class)
          .annotatedWith(Solr.class)
          .to(NameUsageIndexServiceSolr.class)
          .in(Scopes.SINGLETON);
    }
    expose(DatasetImportService.class).annotatedWith(Solr.class);
  }
}
