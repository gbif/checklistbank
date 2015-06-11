package org.gbif.checklistbank.nub.source;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.nub.model.NubTags;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class ClbUsageSourceTest {

  private static final UUID CHECKLIST_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");
  private static ClbConfiguration cfg = new ClbConfiguration();
  private ClbUsageSource src;
  private DatasetService ds;

  @Rule
  public DatabaseDrivenChecklistBankTestRule<NameUsageService> ddt =
    new DatabaseDrivenChecklistBankTestRule<NameUsageService>(NameUsageService.class);

  @Before
  public void init () {
    ds = Mockito.mock(DatasetService.class);
    Dataset squirrel = new Dataset();
    squirrel.setKey(CHECKLIST_KEY);
    squirrel.setTitle("Squirrels");
    squirrel.addMachineTag( new MachineTag(NubTags.NAMESPACE, NubTags.PRIORITY.tag, "10") );
    squirrel.addMachineTag( new MachineTag(NubTags.NAMESPACE, NubTags.RANK_LIMIT.tag, "KINGDOM") );
    PagingResponse<Dataset> resp = new PagingResponse<Dataset>();
    resp.setCount(1l);
    resp.getResults().add(squirrel);

    when(ds.listByType(any(DatasetType.class), any(PagingRequest.class))).thenReturn(resp);

    // use default prod API
    Properties props = ddt.getProperties();
    cfg.databaseName = props.getProperty("checklistbank.db.dataSource.databaseName");
    cfg.serverName = props.getProperty("checklistbank.db.dataSource.serverName");
    cfg.user = props.getProperty("checklistbank.db.dataSource.user");
    cfg.password = props.getProperty("checklistbank.db.dataSource.password");
    src = new ClbUsageSource(ds, cfg);
  }

  /**
   * integration test with prod registry
   * @throws Exception
   */
  @Test
  public void testListSources() throws Exception {
    List<NubSource> sources = src.listSources();
    assertEquals(1, sources.size());
    assertEquals(10, sources.get(0).priority);
    assertEquals(Rank.KINGDOM, sources.get(0).ignoreRanksAbove);
  }

  @Test
  public void testIterateSource() throws Exception {
    NubSource squirrelSource = new NubSource();
    squirrelSource.name = "squirrels";
    squirrelSource.key = CHECKLIST_KEY;
    squirrelSource.ignoreRanksAbove = Rank.SPECIES;
    int counter = 0;
    for (SrcUsage u : src.iterateSource(squirrelSource)) {
      counter++;
      System.out.print(u.key + "  ");
      System.out.println(u.scientificName);
    }
    assertEquals(44, counter);
  }
}