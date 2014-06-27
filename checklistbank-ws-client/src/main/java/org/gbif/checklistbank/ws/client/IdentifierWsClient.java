package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.IdentifierService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWs;
import org.gbif.checklistbank.ws.util.Constants;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * Client-side implementation to the DistributionService.
 */
public class IdentifierWsClient extends NameUsageComponentBaseWsClient<Identifier> implements IdentifierService {

  @Inject
  public IdentifierWsClient(@ChecklistBankWs WebResource resource) {
    super(Identifier.class, new GenericType<PagingResponse<Identifier>>() {
    }, resource, Constants.IDENTIFIER_PATH);
  }
}
