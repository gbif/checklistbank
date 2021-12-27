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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class DatasetImportServiceMyBatisIT extends MyBatisServiceITBase {

  private final DatasetImportService service;
  private final NameUsageService nameUsageService;

  private final Integer USAGE_ID = 555555;

  @Autowired
  public DatasetImportServiceMyBatisIT(
      DataSource dataSource,
      DatasetImportService datasetImportService,
      NameUsageService nameUsageService) {
    super(dataSource);
    this.service = datasetImportService;
    this.nameUsageService = nameUsageService;
  }

  /** pro parte usages are synced differently and not tested here */
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

    NameUsage u2 = nameUsageService.get(USAGE_ID, null);

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

    DummyData(
        NameUsage u, ParsedName pn, UsageExtensions ext, VerbatimNameUsage v, NameUsageMetrics m) {
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
    public void reportUsageKey(long id, int usageKey) {}

    @Override
    public void reportNewFuture(Future<List<Integer>> future) {}
  }
}
