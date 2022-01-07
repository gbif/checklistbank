/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
