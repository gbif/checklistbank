/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.index.guice;

import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.checklistbank.index.service.NameUsageSearchServiceImpl;
import org.gbif.common.search.inject.SolrModule;
import org.gbif.service.guice.PrivateServiceModule;

import java.util.Properties;

import com.google.inject.Scopes;

/**
 * A search module that can be configured to use an externally bound solr instance or to insert one
 * based on the given properties.
 */
public class SearchModule extends PrivateServiceModule {

  private static final String CLB_PROPERTY_PREFIX = "checklistbank.search.";
  private final boolean installSolr;

  /**
   * Alternative constructor allowing an existing and guice bound solr server to be used.
   * Useful for tests.
   *
   * @param installSolr if true a new solr server is bound, if false an existing binding is expected.
   */
  public SearchModule(Properties properties, final boolean installSolr) {
    super(CLB_PROPERTY_PREFIX, properties);
    this.installSolr = installSolr;
  }

  @Override
  protected void configureService() {
    // do we have solr already?
    if (installSolr) {
      // bind solr server, using the checklistbank.search.solr.server property
      install(new SolrModule());
    }

    bind(NameUsageSearchService.class).to(NameUsageSearchServiceImpl.class).in(Scopes.SINGLETON);

    expose(NameUsageSearchService.class);
  }
}
