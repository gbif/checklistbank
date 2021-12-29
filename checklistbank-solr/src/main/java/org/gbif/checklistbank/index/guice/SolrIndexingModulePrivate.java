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

import org.gbif.checklistbank.index.NameUsageDocConverter;
import org.gbif.checklistbank.index.backfill.IndexingConfigKeys;
import org.gbif.checklistbank.index.backfill.SolrBackfill;
import org.gbif.common.search.solr.SolrConfig;
import org.gbif.common.search.solr.SolrModule;
import org.gbif.service.guice.PrivateServiceModule;

import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;


/**
 * Guice module that bind the indexing classes.
 * This module depends on the checklist bank mybatis modules to be installed too
 * which is done in the {@link SolrIndexingModule}.
 */
public class SolrIndexingModulePrivate extends PrivateServiceModule {

  public SolrIndexingModulePrivate(Properties properties) {
    super(IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX, properties);
  }

  @Override
  protected void configureService() {
    //Setting this property because the default value in the solrconfig.xml is solr.lock.type=hdfsSystem.setProperty("solr.lock.type", "native");
    // Object <-> Document converter binding
    bind(NameUsageDocConverter.class).in(Scopes.SINGLETON);

    // Main indexer class
    bind(SolrBackfill.class).in(Scopes.SINGLETON);

    install(new SolrModule(SolrConfig.fromProperties(getProperties(), "solr.")));

    expose(SolrBackfill.class);
    expose(SolrClient.class);
    expose(EmbeddedSolrReference.class);
  }

  @Inject
  @Singleton
  @Provides
  public EmbeddedSolrReference provideSolrReference(SolrClient solrClient) {
    return new EmbeddedSolrReference(solrClient);
  }
}
