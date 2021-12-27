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
package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.service.DatasetAnalysisService;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;

import java.util.Date;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatasetAnalysisServiceMyBatisIT extends MyBatisServiceITBase {

  private final DatasetAnalysisService service;

  @Autowired
  public DatasetAnalysisServiceMyBatisIT(
      DataSource dataSource, DatasetAnalysisService datasetAnalysisService) {
    super(dataSource);
    this.service = datasetAnalysisService;
  }

  @Test
  public void testAnalyse() {
    final Date downloaded = new Date();
    DatasetMetrics d = service.analyse(ClbDbTestRule.SQUIRRELS_DATASET_KEY, downloaded);
    System.out.println(d);
    assertEquals(ClbDbTestRule.SQUIRRELS_DATASET_KEY, d.getDatasetKey());
    assertEquals(downloaded, d.getDownloaded());
    assertEquals(44, d.getUsagesCount());
    assertEquals(16, d.getSynonymsCount());
    assertEquals(44, d.getDistinctNamesCount());
    assertEquals(2, d.getNubMatchingCount());
    assertEquals(4, d.getNubCoveragePct());
    assertEquals(0, d.getColCoveragePct());
    assertEquals(0, d.getColMatchingCount());
    assertEquals(1, d.getCountByKingdom().size());
    // there are more animal records in this dataset, but only 2 are mapped to the nub!
    assertEquals(2, d.getCountByKingdom(Kingdom.ANIMALIA));
    assertEquals(0, d.getCountByKingdom(Kingdom.PLANTAE));
    assertEquals(10, d.getCountByRank(Rank.SUBSPECIES));
    assertEquals(3, d.getCountByRank(Rank.SPECIES));
    assertEquals(2, d.getCountByRank(Rank.GENUS));
    assertEquals(1, d.getCountByRank(Rank.PHYLUM));
    assertEquals(4, d.getCountNamesByLanguage(Language.ENGLISH));
    assertEquals(2, d.getCountNamesByLanguage(Language.GERMAN));
  }
}
