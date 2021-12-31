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
package org.gbif.checklistbank.index.backfill;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.service.checklistbank.*;
import org.gbif.checklistbank.index.BaseIT;
import org.gbif.checklistbank.index.NameUsageDocConverter;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.ClbDbLoadTestDataBeforeAll;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.TestData;
import org.gbif.checklistbank.service.mybatis.service.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.VernacularNameServiceMyBatis;

import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Do a doc conversion using a real db. Manual test only to debug problems.
 */
@ExtendWith(ClbDbLoadTestDataBeforeAll.class)
@TestData(TestData.DATAFILE.SQUIRRELS)
public class NameUsageIndexingJobIT extends BaseIT {

  private final UsageService nameUsageService;
  private final VernacularNameServiceMyBatis vernacularNameService;
  private final DescriptionServiceMyBatis descriptionService;
  private final DistributionServiceMyBatis distributionService;
  private final SpeciesProfileServiceMyBatis speciesProfileService;
  private final SolrClient solrClient;
  private final NameUsageSearchService searchService;

  @Autowired
  public NameUsageIndexingJobIT(
    UsageService nameUsageService,
    VernacularNameServiceMyBatis vernacularNameService,
    DescriptionServiceMyBatis descriptionService,
    DistributionServiceMyBatis distributionService,
    SpeciesProfileServiceMyBatis speciesProfileService,
    SolrClient solrClient,
    NameUsageSearchService searchService
  ) {
    this.nameUsageService = nameUsageService;
    this.vernacularNameService = vernacularNameService;
    this.descriptionService = descriptionService;
    this.distributionService = distributionService;
    this.speciesProfileService = speciesProfileService;
    this.solrClient = solrClient;
    this.searchService = searchService;
  }

  @Test
  public void testSpeciesInfoRange() throws Exception {
    int start = 114989960;
    int end   = 114989970;
    Map<Integer, List<SpeciesProfile>> range = speciesProfileService.listRange(start, end);
    assertEquals(0, range.size());
  }

  @Test
  @Disabled("Test data seems wrong")
  public void testDescriptionRange() throws Exception {
    int start = 100000000;
    int end   = 100000025;
    Map<Integer, List<Description>> range = descriptionService.listRange(start, end);
    assertEquals(3, range.size());
    assertEquals(6, range.get(100000025).size());
  }

  @Test
  @Disabled("Test data seems wrong")
  public void testDistributionRange() throws Exception {
    int start = 100000040;
    int end   = 101000000;
    Map<Integer, List<Distribution>> range = distributionService.listRange(start, end);
    assertEquals(1, range.size());
    assertEquals(10, range.get(100000040).size());
  }

  @Test
  public void testCall() throws Exception {
    int start = 114989960;
    int end   = 114989970;
    start = 110448010;
    end   = 110448020;
    NameUsageIndexingJob job = new NameUsageIndexingJob(solrClient, nameUsageService, start, end, new NameUsageDocConverter(),
      vernacularNameService, descriptionService, distributionService, speciesProfileService);
    job.call();

    solrClient.commit();

  }
}
