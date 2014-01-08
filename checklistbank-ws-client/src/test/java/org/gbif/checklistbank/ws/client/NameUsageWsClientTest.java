package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.NameUsage;

import java.util.Locale;
import java.util.UUID;

import com.sun.jersey.api.client.WebResource;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class NameUsageWsClientTest extends WsClientBaseTest<NameUsage> {

  private NameUsageWsClient client;
  private Locale locale = new Locale("en", "US");
  private final UUID datasetKey = UUID.randomUUID();

  public NameUsageWsClientTest() {
    super(NameUsage.class);
  }

  @Override
  NameUsage getMockObject() {
    NameUsage usage = new NameUsage();
    usage.setDatasetKey(datasetKey);
    usage.setKey(key);
    return usage;
  }

  @Override
  NameUsageWsClient getClient(WebResource resource) {
    return new NameUsageWsClient(resource);
  }

  @Before
  @Override
  public void setUp() {
    super.setUp();
    client = getClient(resource);
    when(resource.get(client.tPage)).thenReturn(getMockResponse());
    when(resourceBuilder.get(eq(client.tPage))).thenReturn(getMockResponse());
  }

  @Test
  public void testGetLocale() {
    int key = 1111;
    NameUsage obj = client.get(key, locale);
    assertEquals(getMockObject(), obj);
  }

  @Test
  public void testListRoot() {
    client.listRoot(datasetKey, locale, page);
  }

  @Test
  public void testList() {
    client.list(locale, null, null, page);
  }

  @Test
  public void testListChildren() {
    int parentKey = 2222;
    client.listChildren(parentKey, locale, page);
  }

  @Test
  public void testListSynonyms() {
    int key = 1111;
    client.listSynonyms(key, locale, page);
  }

}
