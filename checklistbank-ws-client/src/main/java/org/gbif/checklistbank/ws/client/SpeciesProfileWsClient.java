package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWs;
import org.gbif.checklistbank.ws.util.Constants;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * Client-side implementation to the SpeciesProfileService.
 */
public class SpeciesProfileWsClient extends NameUsageComponentBaseWsClient<SpeciesProfile>
  implements SpeciesProfileService {

  @Inject
  public SpeciesProfileWsClient(@ChecklistBankWs WebResource resource) {
    super(SpeciesProfile.class, new GenericType<PagingResponse<SpeciesProfile>>() {
    }, resource, Constants.SPECIES_PROFILES_PATH);
  }
}
