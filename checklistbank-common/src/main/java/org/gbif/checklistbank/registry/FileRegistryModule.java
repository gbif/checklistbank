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
