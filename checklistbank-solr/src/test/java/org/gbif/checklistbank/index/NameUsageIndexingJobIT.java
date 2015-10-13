package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.VernacularNameServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.common.search.solr.builders.EmbeddedServerBuilder;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

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

  @Before
  public void init() throws IOException {
    Properties props = PropertiesUtil.loadProperties("checklistbank.properties");
    props.list(System.out);
    Injector inj = Guice.createInjector(new ChecklistBankServiceMyBatisModule(props));
    nameUsageService = inj.getInstance(UsageService.class);
    vernacularNameService = (VernacularNameServiceMyBatis) inj.getInstance(VernacularNameService.class);
    descriptionService = (DescriptionServiceMyBatis) inj.getInstance(DescriptionService.class);
    distributionService = (DistributionServiceMyBatis) inj.getInstance(DistributionService.class);
    speciesProfileService = (SpeciesProfileServiceMyBatis) inj.getInstance(SpeciesProfileService.class);
    solrClient = new EmbeddedServerBuilder().build();
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
    int start = 110448010;
    int end   = 110448020;
    Map<Integer, List<Description>> range = descriptionService.listRange(start, end);
    assertEquals(10, range.size());
  }

  @Test
  public void testDistributionRange() throws Exception {
    int start = 110448010;
    int end   = 110448020;
    Map<Integer, List<Distribution>> range = distributionService.listRange(start, end);
    assertEquals(0, range.size());
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
  }
}
