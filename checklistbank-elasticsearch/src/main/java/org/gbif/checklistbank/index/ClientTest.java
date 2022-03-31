package org.gbif.checklistbank.index;

import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

public class ClientTest {

  public static void main(String[] args) {
    OccurrenceCountClient  client = new ClientBuilder().withUrl("https://api.gbif.org/v1/").withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport()).build(OccurrenceCountClient.class);
    System.out.println(client.count(2435098));
  }
}
