package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.SpeciesProfile;

import com.sun.jersey.api.client.WebResource;

public class SpeciesProfileWsClientTest extends WsClientNameUsageComponentTest<SpeciesProfile> {

  public SpeciesProfileWsClientTest() {
    super(SpeciesProfile.class);
  }

  @Override
  SpeciesProfile getMockObject() {
    SpeciesProfile speciesProfile = new SpeciesProfile();
    speciesProfile.setExtinct(true);
    return speciesProfile;
  }

  @Override
  NameUsageComponentBaseWsClient<SpeciesProfile> getClient(WebResource resource) {
    return new SpeciesProfileWsClient(resource);
  }

}
