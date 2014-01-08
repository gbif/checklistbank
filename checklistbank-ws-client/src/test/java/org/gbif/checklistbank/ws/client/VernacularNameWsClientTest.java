package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.VernacularName;

import com.sun.jersey.api.client.WebResource;

public class VernacularNameWsClientTest extends WsClientNameUsageComponentTest<VernacularName> {

  public VernacularNameWsClientTest() {
    super(VernacularName.class);
  }

  @Override
  VernacularName getMockObject() {
    VernacularName vernacularName = new VernacularName();
    vernacularName.setVernacularName("Red squirrel");
    return vernacularName;
  }

  @Override
  NameUsageComponentBaseWsClient<VernacularName> getClient(WebResource resource) {
    return new VernacularNameWsClient(resource);
  }
}
