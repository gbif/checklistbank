package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.TypeSpecimenService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWs;
import org.gbif.checklistbank.ws.util.Constants;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * Client-side implementation to the TypeSpecimenService.
 */
public class TypeSpecimenWsClient extends NameUsageComponentBaseWsClient<TypeSpecimen> implements TypeSpecimenService {

  @Inject
  public TypeSpecimenWsClient(@ChecklistBankWs WebResource resource) {
    super(TypeSpecimen.class, new GenericType<PagingResponse<TypeSpecimen>>() {
    }, resource, Constants.TYPE_SPECIMENS_PATH, Constants.TYPE_SPECIMEN_PATH);
  }
}
