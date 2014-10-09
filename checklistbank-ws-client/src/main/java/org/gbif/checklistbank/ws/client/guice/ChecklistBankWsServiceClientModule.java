/*
 * Copyright 2011 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.checklistbank.ws.client.guice;

import org.gbif.api.service.checklistbank.DatasetMetricsService;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.IdentifierService;
import org.gbif.api.service.checklistbank.MultimediaService;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.service.checklistbank.ReferenceService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.TypeSpecimenService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.ws.client.DatasetMetricsWsClient;
import org.gbif.checklistbank.ws.client.DescriptionWsClient;
import org.gbif.checklistbank.ws.client.DistributionWsClient;
import org.gbif.checklistbank.ws.client.IdentifierWsClient;
import org.gbif.checklistbank.ws.client.MultimediaWsClient;
import org.gbif.checklistbank.ws.client.NameUsageSearchWsClient;
import org.gbif.checklistbank.ws.client.NameUsageWsClient;
import org.gbif.checklistbank.ws.client.ReferenceWsClient;
import org.gbif.checklistbank.ws.client.SpeciesProfileWsClient;
import org.gbif.checklistbank.ws.client.TypeSpecimenWsClient;
import org.gbif.checklistbank.ws.client.VernacularNameWsClient;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 * Guice module that includes implementations for all the ChecklistBank services including the NameUsageSearchService.
 * It required the ChecklistBankHttpClient annotated jersey client and should not be used on its own.
 * Use the main #ChecklistBankWsClientModule(false,true) instead with its configurable constructor.
 * The only things left for clients to do is:
 * 1. Bind the Service class corresponding to the Client's class in configure()
 * 2. Create a method that returns a WebResource object. The method must:
 * a) Use the @Provides annotation, @singleton annotation, and the Client's specific binding annotation.
 * b) Include the @ChecklistBankHttpClient binding annotation for the the Jersey Client param.
 * c) Where possible, the WebResource returned should define its path. Our convention uses the named annotation
 * "checklistbank.ws.url" (passed in as a parameter to the method) as the base URL.
 * Example path for ChecklistWs would be: Names.named("checklistbank.ws.url") + "species"
 */
public class ChecklistBankWsServiceClientModule extends AbstractModule {

  /**
   * Use the main #ChecklistBankWsClientModule(false,true) instead.
   */
  protected ChecklistBankWsServiceClientModule() {
  }

  @Override
  protected void configure() {
    // search
    bind(NameUsageSearchService.class).to(NameUsageSearchWsClient.class).in(Scopes.SINGLETON);
    // usages
    bind(NameUsageService.class).to(NameUsageWsClient.class).in(Scopes.SINGLETON);
    // usage components
    bind(DescriptionService.class).to(DescriptionWsClient.class).in(Scopes.SINGLETON);
    bind(DistributionService.class).to(DistributionWsClient.class).in(Scopes.SINGLETON);
    bind(IdentifierService.class).to(IdentifierWsClient.class).in(Scopes.SINGLETON);
    bind(MultimediaService.class).to(MultimediaWsClient.class).in(Scopes.SINGLETON);
    bind(ReferenceService.class).to(ReferenceWsClient.class).in(Scopes.SINGLETON);
    bind(SpeciesProfileService.class).to(SpeciesProfileWsClient.class).in(Scopes.SINGLETON);
    bind(TypeSpecimenService.class).to(TypeSpecimenWsClient.class).in(Scopes.SINGLETON);
    bind(VernacularNameService.class).to(VernacularNameWsClient.class).in(Scopes.SINGLETON);
    // metrics
    bind(DatasetMetricsService.class).to(DatasetMetricsWsClient.class).in(Scopes.SINGLETON);
  }

  /**
   * @return the web resource to the name usage base url
   */
  @Provides
  @Singleton
  @ChecklistBankWs
  private WebResource providesNameUsageWsWebResource(Client client, @Named("checklistbank.ws.url") String url) {
    return client.resource(url);
  }

}
