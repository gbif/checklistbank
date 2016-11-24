package org.gbif.checklistbank.registry;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;

import java.io.File;

import com.google.inject.AbstractModule;

/**
 *
 */
public class FileRegistryModule extends AbstractModule {
  private final File datasetFile;

  public FileRegistryModule(File datasetFile) {
    this.datasetFile = datasetFile;
  }

  @Override
  protected void configure() {
    bind(DatasetService.class).toInstance(new DatasetServiceFileImpl(datasetFile));
    bind(OrganizationService.class).toInstance(new OrganizationServiceEmptyImpl());
    bind(InstallationService.class).toInstance(new InstallationServiceEmptyImpl());
    bind(NetworkService.class).toInstance(new NetworkServiceEmptyImpl());
    bind(NodeService.class).toInstance(new NodeServiceEmptyImpl());
  }

}
