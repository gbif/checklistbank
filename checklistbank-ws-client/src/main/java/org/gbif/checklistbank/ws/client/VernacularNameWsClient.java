package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWs;
import org.gbif.checklistbank.ws.util.Constants;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * Client-side implementation to the VernacularNameService.
 */
public class VernacularNameWsClient extends NameUsageComponentBaseWsClient<VernacularName>
  implements VernacularNameService {

  @Inject
  public VernacularNameWsClient(@ChecklistBankWs WebResource resource) {
    super(VernacularName.class, new GenericType<PagingResponse<VernacularName>>() {
    }, resource, Constants.VERNACULAR_NAMES_PATH);
  }
}
