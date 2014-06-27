package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.Description;

import com.sun.jersey.api.client.WebResource;

public class DescriptionWsClientTest extends WsClientNameUsageComponentTest<Description> {

  public DescriptionWsClientTest() {
    super(Description.class);
  }

  @Override
  Description getMockObject() {
    Description description = new Description();
    description.setType("lifespan");
    description.setDescription("12 days");
    return description;
  }

  @Override
  NameUsageComponentBaseWsClient<Description> getClient(WebResource resource) {
    return new DescriptionWsClient(resource);
  }

}
