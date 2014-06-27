package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.NameUsageMediaObject;

import java.net.URI;

import com.sun.jersey.api.client.WebResource;

public class MultimediaWsClientTest extends WsClientNameUsageComponentTest<NameUsageMediaObject> {

  public MultimediaWsClientTest() {
    super(NameUsageMediaObject.class);
  }

  @Override
  NameUsageMediaObject getMockObject() {
    NameUsageMediaObject image = new NameUsageMediaObject();
    image.setIdentifier(URI.create("http://imageurl.jpg"));
    return image;
  }

  @Override
  NameUsageComponentBaseWsClient<NameUsageMediaObject> getClient(WebResource resource) {
    return new MultimediaWsClient(resource);
  }

}
