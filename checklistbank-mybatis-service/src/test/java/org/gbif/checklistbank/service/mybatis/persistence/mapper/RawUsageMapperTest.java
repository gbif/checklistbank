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

import org.gbif.checklistbank.model.RawUsage;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** */
public class RawUsageMapperTest extends MapperITBase {

  private final RawUsageMapper mapper;

  @Autowired
  public RawUsageMapperTest(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      RawUsageMapper rawUsageMapper) {
    super(
        parsedNameMapper,
        nameUsageMapper,
        nubRelMapper,
        datasetMapper,
        citationMapper,
        true);
    this.mapper = rawUsageMapper;
  }

  @Test
  public void crudTest() {
    final RawUsage raw = new RawUsage();
    raw.setUsageKey(usageKey);
    raw.setJson("{'me':'markus'}");
    raw.setDatasetKey(datasetKey);
    raw.setLastCrawled(new Date());

    assertNull(mapper.get(usageKey));
    mapper.insert(raw);

    // this is taken from dataset_metrics and not stored in each raw usage so null it for comparison
    raw.setLastCrawled(null);
    assertEquals(raw, mapper.get(usageKey));
    mapper.delete(usageKey);
    assertNull(mapper.get(usageKey));
  }
}
