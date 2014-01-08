package org.gbif.checklistbank.ws.client.guice;

import org.gbif.api.service.checklistbank.DatasetMetricsService;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.ImageService;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.service.checklistbank.NameUsageSearchService;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.service.checklistbank.ReferenceService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.TypeSpecimenService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.ws.client.NameUsageWsClient;
import org.gbif.ws.client.guice.GbifWsClientModule;

import java.util.Properties;

/**
 * A complete guice module that exposes the full ChecklistBank API or a configurable subset.
 */
public class ChecklistBankWsClientModule extends GbifWsClientModule {

  private final boolean installSearchClient;
  private final boolean installServiceClients;
  private final boolean installMatchClients;

  /**
   * Default module with the complete API exposed.
   */
  public ChecklistBankWsClientModule(Properties properties) {
    this(properties, true, true, true);
  }

  /**
   * Configurable module with flags to select the API parts to be exposed.
   */
  public ChecklistBankWsClientModule(Properties properties, boolean exposeSearchClient, boolean exposeServiceClients,
    boolean exposeMatchClient) {
    super(properties, NameUsageWsClient.class.getPackage());
    this.installSearchClient = exposeSearchClient;
    this.installServiceClients = exposeServiceClients;
    this.installMatchClients = exposeMatchClient;
  }

  @Override
  protected void configureClient() {
    // install the 2 private modules that together implement the API
    if (installSearchClient) {
      install(new ChecklistBankWsSearchClientModule());
      expose(NameUsageSearchService.class);
    }
    if (installServiceClients) {
      install(new ChecklistBankWsServiceClientModule());
      expose(NameUsageService.class);
      expose(DescriptionService.class);
      expose(DistributionService.class);
      expose(ImageService.class);
      expose(ReferenceService.class);
      expose(SpeciesProfileService.class);
      expose(TypeSpecimenService.class);
      expose(VernacularNameService.class);
      expose(DatasetMetricsService.class);
    }
    if (installMatchClients) {
      install(new ChecklistBankWsMatchClientModule());
      expose(NameUsageMatchingService.class);
    }
  }

}
