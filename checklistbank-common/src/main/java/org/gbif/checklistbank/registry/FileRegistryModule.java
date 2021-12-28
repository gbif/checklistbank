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
package org.gbif.checklistbank.registry;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;

import java.io.File;

import org.springframework.context.annotation.Bean;

public class FileRegistryModule {
  private final File datasetFile;

  public FileRegistryModule(File datasetFile) {
    this.datasetFile = datasetFile;
  }

  @Bean
  public DatasetService datasetServiceFile() {
    return new DatasetServiceFileImpl(datasetFile);
  }

  @Bean
  public OrganizationService organizationService() {
    return new OrganizationServiceEmptyImpl();
  }

  @Bean
  public InstallationService installationService() {
    return new InstallationServiceEmptyImpl();
  }

  @Bean
  public NetworkService networkService() {
    return new NetworkServiceEmptyImpl();
  }

  @Bean
  public NodeService nodeService() {
    return new NodeServiceEmptyImpl();
  }

}
