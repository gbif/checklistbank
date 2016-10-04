package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.ImporterCallback;
import org.gbif.checklistbank.service.mybatis.guice.Mybatis;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class DatasetImportServiceMyBatisIT extends MyBatisServiceITBase<DatasetImportService> {

  public DatasetImportServiceMyBatisIT() {
    super(DatasetImportService.class, Mybatis.class);
  }

  private final Integer USAGE_ID = 555555;

  /**
   * pro parte usages are synced differently and not tested here
   */
  @Test
  public void testRegularImport() throws ExecutionException, InterruptedException {

    ParsedName pn = new ParsedName();
    pn.setGenusOrAbove("Abies");
    pn.setSpecificEpithet("alba");
    pn.setAuthorship("Mill.");
    pn.setScientificName(pn.fullName());
    pn.setType(NameType.SCIENTIFIC);

    NameUsage u = new NameUsage();
    u.setKey(USAGE_ID);
    u.setScientificName(pn.getScientificName());
    u.setCanonicalName(pn.canonicalName());
    u.setNameType(pn.getType());
    u.setAuthorship(pn.getAuthorship());
    u.setRank(Rank.SPECIES);
    u.setDatasetKey(Constants.NUB_DATASET_KEY);
    u.setOrigin(Origin.SOURCE);
    u.getIssues().add(NameUsageIssue.CONFLICTING_BASIONYM_COMBINATION);
    u.getIssues().add(NameUsageIssue.ACCEPTED_NAME_MISSING);

    UsageExtensions ext = new UsageExtensions();
    VerbatimNameUsage v = new VerbatimNameUsage();
    NameUsageMetrics m = new NameUsageMetrics();

    DummyData data = new DummyData(u, pn, ext, v, m);

    Future<?> f = service.sync(u.getDatasetKey(), data, Lists.newArrayList(1));
    f.get();

    NameUsageService usageService = getInstance(NameUsageService.class);
    NameUsage u2 = usageService.get(USAGE_ID, null);

    u2.setLastInterpreted(null);

    // name key is newly assigned
    u.setNameKey(u2.getNameKey());
    u.setNameKey(u2.getNameKey());
    assertNotNull(u.getNameKey());

    assertFalse(u2.getIssues().isEmpty());

    assertEquals(u, u2);
  }

  class DummyData implements ImporterCallback {
    private final NameUsage u;
    private final ParsedName pn;
    private final UsageExtensions ext;
    private final VerbatimNameUsage v;
    private final NameUsageMetrics m;

    DummyData(NameUsage u, ParsedName pn, UsageExtensions ext, VerbatimNameUsage v, NameUsageMetrics m) {
      this.u = u;
      this.pn = pn;
      this.ext = ext;
      this.v = v;
      this.m = m;
    }

    @Override
    public NameUsage readUsage(long id) {
      return u;
    }

    @Override
    public ParsedName readName(long id) {
      return pn;
    }

    @Override
    public boolean isInsert(NameUsage usage) {
      return true;
    }

    @Override
    public UsageExtensions readExtensions(long id) {
      return ext;
    }

    @Override
    public NameUsageMetrics readMetrics(long id) {
      return m;
    }

    @Override
    public VerbatimNameUsage readVerbatim(long id) {
      return v;
    }

    @Override
    public List<Integer> readParentKeys(long id) {
      return Lists.newArrayList();
    }

    @Override
    public void reportUsageKey(long id, int usageKey) {

    }

    @Override
    public void reportNewFuture(Future<List<Integer>> future) {

    }
  }
}
