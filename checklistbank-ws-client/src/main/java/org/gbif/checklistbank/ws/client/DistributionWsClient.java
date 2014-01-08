package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWs;
import org.gbif.checklistbank.ws.util.Constants;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

/**
 * Client-side implementation to the DistributionService.
 */
public class DistributionWsClient extends NameUsageComponentBaseWsClient<Distribution> implements DistributionService {

  @Inject
  public DistributionWsClient(@ChecklistBankWs WebResource resource) {
    super(Distribution.class, new GenericType<PagingResponse<Distribution>>() {
    }, resource, Constants.DISTRIBUTIONS_PATH, Constants.DISTRIBUTION_PATH);
  }
}
