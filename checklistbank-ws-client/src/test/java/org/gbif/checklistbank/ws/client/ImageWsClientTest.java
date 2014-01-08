package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.Image;

import com.sun.jersey.api.client.WebResource;

public class ImageWsClientTest extends WsClientNameUsageComponentTest<Image> {

  public ImageWsClientTest() {
    super(Image.class);
  }

  @Override
  Image getMockObject() {
    Image image = new Image();
    image.setImage("http://imageurl.jpg");
    return image;
  }

  @Override
  NameUsageComponentBaseWsClient<Image> getClient(WebResource resource) {
    return new ImageWsClient(resource);
  }

}
