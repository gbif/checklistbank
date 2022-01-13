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

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.Rank;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class DatasetMetricsClientIT extends BaseClientIT {

  private static final UUID CHECKLIST_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");

  private final DatasetMetricsClient client;

  public DatasetMetricsClientIT() throws IOException {
    client =
        new ClientBuilder()
            .withUrl(apiUrl)
            .withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport())
            .build(DatasetMetricsClient.class);
  }

  @Test
  public void testGet() {
    DatasetMetrics d = client.get(CHECKLIST_KEY);
    assertEquals(CHECKLIST_KEY, d.getDatasetKey());
    assertEquals(1000, d.getUsagesCount());
    assertEquals(25, d.getColCoveragePct());
    assertEquals(250, d.getColMatchingCount());
    assertEquals(100, d.getCountByKingdom(Kingdom.ANIMALIA));
    assertEquals(700, d.getCountByKingdom(Kingdom.PLANTAE));
    assertEquals(0, d.getCountByKingdom(Kingdom.FUNGI));
    assertEquals(120, d.getCountByRank(Rank.GENUS));
    assertEquals(10, d.getCountByRank(Rank.PHYLUM));
    assertEquals(4, d.getCountNamesByLanguage(Language.DANISH));
    assertEquals(132, d.getCountNamesByLanguage(Language.GERMAN));
  }

  @Test
  public void testList() {
    List<DatasetMetrics> ds = client.list(CHECKLIST_KEY);
    assertEquals(3, ds.size());
    for (DatasetMetrics d : ds) {
      assertEquals(CHECKLIST_KEY, d.getDatasetKey());
    }
    assertEquals(1000, ds.get(0).getUsagesCount());
    assertEquals(200, ds.get(1).getUsagesCount());
    assertEquals(100, ds.get(2).getUsagesCount());
  }
}
