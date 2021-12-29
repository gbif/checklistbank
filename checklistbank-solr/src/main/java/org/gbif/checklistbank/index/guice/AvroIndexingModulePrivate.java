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

import org.gbif.checklistbank.index.backfill.AvroExporter;
import org.gbif.checklistbank.index.backfill.IndexingConfigKeys;
import org.gbif.service.guice.PrivateServiceModule;

import java.util.Properties;

import com.google.inject.Scopes;
/**
 * Guice module that bind the indexing classes.
 * This module depends on the registry client and checklist bank mybatis modules to be installed too
 * which is done in the {@link org.gbif.checklistbank.index.guice.SolrIndexingModule}.
 */
public class AvroIndexingModulePrivate extends PrivateServiceModule {

  public AvroIndexingModulePrivate(Properties properties) {
    super(IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX, properties);
  }

  @Override
  protected void configureService() {
    //expose the Avro exporter
    bind(AvroExporter.class).in(Scopes.SINGLETON);

    expose(AvroExporter.class);
  }
}
