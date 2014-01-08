package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.ws.client.BaseWsGetClient;

import java.util.List;
import java.util.Locale;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.collect.Lists;
import com.sun.jersey.api.client.WebResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest(WebResource.Builder.class)
public abstract class WsClientBaseTest<T> {

  protected final Class<T> resourceClass;
  protected WebResource resource;
  protected WebResource.Builder resourceBuilder;
  private BaseWsGetClient<T, Integer> client;
  protected final Pageable page = new PagingRequest(0l, 1);
  protected static final int key = 1111;

  public WsClientBaseTest(Class<T> resourceClass) {
    this.resourceClass = resourceClass;
  }

  @Before
  public void setUp() {
    resource = mock(WebResource.class);
    when(resource.path(Matchers.<String>any())).thenReturn(resource);
    when(resource.queryParam(Matchers.<String>any(), Matchers.<String>any())).thenReturn(resource);
    when(resource.queryParams(Matchers.<MultivaluedMap<String, String>>any())).thenReturn(resource);

    resourceBuilder = mock(WebResource.Builder.class);
    when(resource.type(eq(MediaType.APPLICATION_JSON))).thenReturn(resourceBuilder);

    when(resourceBuilder.acceptLanguage(Matchers.<Locale>any())).thenReturn(resourceBuilder);
    when(resourceBuilder.type(eq(MediaType.APPLICATION_JSON))).thenReturn(resourceBuilder);

    client = getClient(resource);
    when(resource.get(resourceClass)).thenReturn(getMockObject());
    when(resourceBuilder.get(eq(resourceClass))).thenReturn(getMockObject());
  }

  protected PagingResponse<T> getMockResponse() {
    List<T> results = Lists.newArrayList();
    T obj = getMockObject();
    results.add(obj);

    return new PagingResponse<T>(page, 1L, results);
  }

  /**
   * @return mocked object with key and usageKey being set in this class
   */
  abstract T getMockObject();

  abstract BaseWsGetClient<T, Integer> getClient(WebResource resource);

  @Test
  public void testGet() {
    T obj = client.get(key);
    T obj2 = getMockObject();
    assertEquals(obj2, obj);
  }

}
