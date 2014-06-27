package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.MultimediaService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWs;
import org.gbif.checklistbank.ws.util.Constants;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * Client-side implementation to the ImageService.
 */
public class MultimediaWsClient extends NameUsageComponentBaseWsClient<NameUsageMediaObject>
    implements MultimediaService {

  @Inject
  public MultimediaWsClient(@ChecklistBankWs WebResource resource) {
    super(NameUsageMediaObject.class, new GenericType<PagingResponse<NameUsageMediaObject>>() {
    }, resource, Constants.MEDIA_PATH);
  }
}
