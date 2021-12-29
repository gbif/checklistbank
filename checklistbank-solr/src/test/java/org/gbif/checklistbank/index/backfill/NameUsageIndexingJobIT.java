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
import org.gbif.checklistbank.index.NameUsageDocConverter;
import org.gbif.checklistbank.index.SolrIndexingTestModule;
import org.gbif.checklistbank.index.guice.SearchModule;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.service.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.VernacularNameServiceMyBatis;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import static org.junit.Assert.assertEquals;

/**
 * Do a doc conversion using a real db. Manual test only to debug problems.
 */
@Ignore
public class NameUsageIndexingJobIT {
  private UsageService nameUsageService;
  private VernacularNameServiceMyBatis vernacularNameService;
  private DescriptionServiceMyBatis descriptionService;
  private DistributionServiceMyBatis distributionService;
  private SpeciesProfileServiceMyBatis speciesProfileService;
  private SolrClient solrClient;
  private NameUsageSearchService searchService;

  @Before
  public void init() throws IOException {
    Properties props = PropertiesUtil.loadProperties(IndexingConfigKeys.CLB_PROPERTY_FILE);
    Properties props2 = PropertiesUtil.loadProperties(IndexingConfigKeys.CLB_INDEXING_PROPERTY_TEST_FILE);
    props.putAll(props2);
    props.list(System.out);

    Injector inj = Guice.createInjector(new SolrIndexingTestModule(props), new SearchModule(props, false));
    nameUsageService = inj.getInstance(UsageService.class);
    vernacularNameService = (VernacularNameServiceMyBatis) inj.getInstance(VernacularNameService.class);
    descriptionService = (DescriptionServiceMyBatis) inj.getInstance(DescriptionService.class);
    distributionService = (DistributionServiceMyBatis) inj.getInstance(DistributionService.class);
    speciesProfileService = (SpeciesProfileServiceMyBatis) inj.getInstance(SpeciesProfileService.class);

    // Get solr
    solrClient = inj.getInstance(SolrClient.class);
    searchService = inj.getInstance(NameUsageSearchService.class);
  }


  @Test
  public void testSpeciesInfoRange() throws Exception {
    int start = 114989960;
    int end   = 114989970;
    Map<Integer, List<SpeciesProfile>> range = speciesProfileService.listRange(start, end);
    assertEquals(0, range.size());
  }

  @Test
  public void testDescriptionRange() throws Exception {
    int start = 100000000;
    int end   = 100000025;
    Map<Integer, List<Description>> range = descriptionService.listRange(start, end);
    assertEquals(3, range.size());
    assertEquals(6, range.get(100000025).size());
  }

  @Test
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
