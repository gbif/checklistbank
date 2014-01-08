package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.Reference;

import com.sun.jersey.api.client.WebResource;

public class ReferenceWsClientTest extends WsClientNameUsageComponentTest<Reference> {

  public ReferenceWsClientTest() {
    super(Reference.class);
  }

  @Override
  Reference getMockObject() {
    Reference reference = new Reference();
    reference.setCitation("Citation string");
    return reference;
  }

  @Override
  NameUsageComponentBaseWsClient<Reference> getClient(WebResource resource) {
    return new ReferenceWsClient(resource);
  }
}
