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

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.model.ParsedNameUsage;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbLoadTestDb;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.TestData;

import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestData(TestData.DATAFILE.SQUIRRELS)
public class NameUsageMapperWithDataIT extends MapperITBase {

  private final NameUsageMapper mapper;

  @Autowired
  public NameUsageMapperWithDataIT(
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
    this.mapper = nameUsageMapper;
  }

  class NonEmptyCounter implements Consumer<ParsedNameUsage> {
    public int counter;

    @Override
    public void accept(ParsedNameUsage u) {
      if (u != null) {
        counter++;
        assertNotNull(u.getKey());
        assertNotNull(u.getScientificName());
        assertNotNull(u.getRank());
        assertNotNull(u.getDatasetKey());

        assertNotNull(u.getParsedName());
        assertNotNull(u.getParsedName().getScientificName());
        System.out.println(u.getParsedName());
      }
    }
  }

  @Transactional
  @Test
  public void testProcessDataset() {
    NonEmptyCounter proc = new NonEmptyCounter();
    mapper.processDataset(UUID.randomUUID()).forEach(proc);
    assertEquals(0, proc.counter);

    mapper.processDataset(Constants.NUB_DATASET_KEY).forEach(proc);
    assertEquals(2, proc.counter);

    mapper.processDataset(ClbLoadTestDb.SQUIRRELS_DATASET_KEY).forEach(proc);
    // we did not reset counter, so it adds up
    assertEquals(46, proc.counter);
  }
}
