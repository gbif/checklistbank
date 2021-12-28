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
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;

import java.util.UUID;

import javax.sql.DataSource;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class VernacularNameMapperWithDataIT extends MapperITBase {

  private final VernacularNameMapper mapper;

  @Autowired
  public VernacularNameMapperWithDataIT(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      VernacularNameMapper vernacularNameMapper,
      DataSource dataSource) {
    super(
      parsedNameMapper,
      nameUsageMapper,
      nubRelMapper,
      datasetMapper,
      citationMapper,
      dataSource,
      false,
      ClbDbTestRule.squirrels(dataSource));
    this.mapper = vernacularNameMapper;
  }

  class NonEmptyCounter implements ResultHandler<VernacularName> {
    public int counter;

    @Override
    public void handleResult(ResultContext<? extends VernacularName> resultContext) {
      if (resultContext.getResultObject() != null) {
        counter++;
        VernacularName d = resultContext.getResultObject();
        System.out.println(d);
        assertNotNull(d.getTaxonKey());
        assertNotNull(d.getVernacularName());
      }
    }
  }

  @Test
  public void testProcessDataset() {
    NonEmptyCounter proc = new NonEmptyCounter();
    mapper.processDataset(UUID.randomUUID(), proc);
    assertEquals(0, proc.counter);

    mapper.processDataset(Constants.NUB_DATASET_KEY, proc);
    assertEquals(0, proc.counter);

    mapper.processDataset(ClbDbTestRule.SQUIRRELS_DATASET_KEY, proc);
    assertEquals(6, proc.counter);
  }
}
