package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.vocabulary.Language;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Disabled
public class DescriptionClientIT extends BaseClientIT {

  private final DescriptionClient client;

  protected DescriptionClientIT() throws IOException {
    client =
        new ClientBuilder()
            .withUrl(apiUrl)
            .withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport())
            .build(DescriptionClient.class);
  }

  @Test
  public void testGet() {
    final Integer key = 14;
    Description d = client.get(key);
    assertEquals("introduction", d.getType());
    assertEquals(Language.ENGLISH, d.getLanguage());

    assertNull(client.get(-2));
  }
}
