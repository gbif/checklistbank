package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.Image;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.ImageService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWs;
import org.gbif.checklistbank.ws.util.Constants;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * Client-side implementation to the ImageService.
 */
public class ImageWsClient extends NameUsageComponentBaseWsClient<Image> implements ImageService {

  @Inject
  public ImageWsClient(@ChecklistBankWs WebResource resource) {
    super(Image.class, new GenericType<PagingResponse<Image>>() {
    }, resource, Constants.IMAGES_PATH, Constants.IMAGE_PATH);
  }
}
