package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWs;
import org.gbif.checklistbank.ws.util.Constants;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * Client-side implementation to the DescriptionService.
 */
public class DescriptionWsClient extends NameUsageComponentBaseWsClient<Description> implements DescriptionService {

  @Inject
  public DescriptionWsClient(@ChecklistBankWs WebResource resource) {
    super(Description.class, new GenericType<PagingResponse<Description>>() {
    }, resource, Constants.DESCRIPTIONS_PATH, Constants.DESCRIPTION_PATH);
  }

}
