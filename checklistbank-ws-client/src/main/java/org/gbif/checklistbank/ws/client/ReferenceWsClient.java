package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.ReferenceService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWs;
import org.gbif.checklistbank.ws.util.Constants;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * Client-side implementation to the ReferenceService.
 */
public class ReferenceWsClient extends NameUsageComponentBaseWsClient<Reference> implements ReferenceService {

  @Inject
  public ReferenceWsClient(@ChecklistBankWs WebResource resource) {
    super(Reference.class, new GenericType<PagingResponse<Reference>>() {
    }, resource, Constants.REFERENCES_PATH);
  }
}
