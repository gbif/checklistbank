package org.gbif.checklistbank.index;

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
import org.apache.solr.client.solrj.SolrServer;
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
  private SolrServer solr;

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
    solr = new EmbeddedServerBuilder().build();
  }

  @Test
  public void testSpeciesInfoRange() throws Exception {
    int start = 109584190;
    int end   = 109584210;
    Map<Integer, List<SpeciesProfile>> sps = speciesProfileService.listRange(start, end);
    assertEquals(0, sps.size());
  }

  @Test
  public void testCall() throws Exception {
    int start = 109584190;
    int end   = 109584210;
    NameUsageIndexingJob job = new NameUsageIndexingJob(solr, nameUsageService, start, end, new NameUsageDocConverter(),
      vernacularNameService, descriptionService, distributionService, speciesProfileService);
    job.call();
  }
}