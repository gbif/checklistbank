package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.TableOfContents;
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
  private final GenericType<TableOfContents> tocType = new GenericType<TableOfContents>() {};

  @Inject
  public DescriptionWsClient(@ChecklistBankWs WebResource resource) {
    super(Description.class, new GenericType<PagingResponse<Description>>() {
    }, resource, Constants.DESCRIPTIONS_PATH);
  }

  @Override
  public TableOfContents getToc(int taxonKey) {
    return get(tocType, Constants.SPECIES_PATH, String.valueOf(taxonKey), "toc");
  }

  /**
   * Returns a component.
   * The object returned is the interpreted version of it.
   *
   * @return A component
   */
  @Override
  public Description get(int key) {
    return get(Constants.DESCRIPTION_PATH, String.valueOf(key));
  }

}
