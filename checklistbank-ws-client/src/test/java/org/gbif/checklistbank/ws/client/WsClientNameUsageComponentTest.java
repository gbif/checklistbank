package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.NameUsageExtension;
import org.gbif.api.model.common.paging.PagingResponse;

import com.sun.jersey.api.client.WebResource;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public abstract class WsClientNameUsageComponentTest<T extends NameUsageExtension> extends WsClientBaseTest<T> {

  protected NameUsageComponentBaseWsClient<T> client;

  public WsClientNameUsageComponentTest(Class<T> resourceClass) {
    super(resourceClass);
  }

  @Before
  @Override
  public void setUp() {
    super.setUp();
    client = getClient(resource);
    when(resource.get(client.tPage)).thenReturn(getMockResponse());
    when(resourceBuilder.get(eq(client.tPage))).thenReturn(getMockResponse());
  }

  @Override
  protected PagingResponse<T> getMockResponse() {
    PagingResponse<T> results = super.getMockResponse();
    for (T obj : results.getResults()) {
      obj.setSourceTaxonKey(key);
    }
    return results;
  }

  @Override
  abstract NameUsageComponentBaseWsClient<T> getClient(WebResource resource);

  @Test
  public void testListByUsage() {
    client.listByUsage(key, page);
  }

}
