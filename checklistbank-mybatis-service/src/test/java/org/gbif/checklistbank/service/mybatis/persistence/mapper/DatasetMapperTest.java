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
package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.checklistbank.model.DatasetCore;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DatasetMapperTest extends MapperITBase {

  private final DatasetMapper mapper;

  @Autowired
  public DatasetMapperTest(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper) {
    super(
        parsedNameMapper,
        nameUsageMapper,
        nubRelMapper,
        datasetMapper,
        citationMapper,
        false);
    this.mapper = datasetMapper;
  }

  @Test
  public void truncateInsertUpdateGet() {
    final DatasetCore d = new DatasetCore();
    final UUID key = UUID.randomUUID();
    d.setKey(key);
    d.setPublisher(UUID.randomUUID());
    d.setParent(UUID.randomUUID());
    d.setTitle("Quadrophenia");

    final DatasetCore d2 = new DatasetCore();
    d2.setKey(key);
    d2.setTitle("Quadrophenia reloaded");
    d2.setPublisher(d.getPublisher());
    d2.setParent(d.getParent());

    mapper.truncate();
    mapper.insert(d);
    assertEquals(d, mapper.get(key));

    mapper.update(d2);
    assertEquals(d2, mapper.get(key));
    assertEquals(d2.getTitle(), mapper.get(key).getTitle());

    mapper.truncate();
    assertNull(mapper.get(key));

    mapper.insert(d);
    assertEquals(d, mapper.get(key));
    mapper.delete(key);
    assertNull(mapper.get(key));
  }
}
