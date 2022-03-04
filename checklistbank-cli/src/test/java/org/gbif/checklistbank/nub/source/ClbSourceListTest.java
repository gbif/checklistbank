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
package org.gbif.checklistbank.nub.source;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.cli.nubbuild.NubSourceConfig;
import org.gbif.checklistbank.nub.NeoTmpRepoRule;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbLoadTestDb;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.AdditionalMatchers;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.neo4j.helpers.collection.Iterables;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class ClbSourceListTest {

  private static final UUID CHECKLIST_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");
  private static final UUID ORG_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d01984");
  private static final UUID INS_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d01986");
  private ClbSourceList src;
  private DatasetService ds;
  private OrganizationService os;
  private InstallationService is;
  private UUID oldDKey;

  @RegisterExtension
  public static NeoTmpRepoRule neoRepo = new NeoTmpRepoRule();

  @RegisterExtension
  public static PreparedDbExtension database =
    EmbeddedPostgresExtension.preparedDatabase(
      LiquibasePreparer.forClasspathLocation("liquibase/checklistbank/master.xml"));

  @RegisterExtension
  public ClbLoadTestDb clbLoadTestDb = ClbLoadTestDb.squirrels(database.getTestDatabase());

  @BeforeEach
  public void init() {
    ds = Mockito.mock(DatasetService.class);
    Dataset squirrel = new Dataset();
    squirrel.setKey(CHECKLIST_KEY);
    squirrel.setTitle("Squirrels");
    squirrel.setType(DatasetType.CHECKLIST);
    when(ds.get(eq(CHECKLIST_KEY))).thenReturn(squirrel);

    os = Mockito.mock(OrganizationService.class);
    Organization org1 = new Organization();
    org1.setKey(ORG_KEY);
    org1.setTitle("Org1");
    when(os.get(eq(ORG_KEY))).thenReturn(org1);

    oldDKey = UUID.randomUUID();
    final Date now = new Date();
    PagingResponse<Dataset> resp3 = new PagingResponse<Dataset>();
    PagingResponse<Dataset> respEmpty = new PagingResponse<Dataset>();
    respEmpty.setCount(0l);
    respEmpty.setEndOfRecords(true);

    Dataset orgD = new Dataset();
    orgD.setKey(oldDKey);
    orgD.setTitle("orgD");
    orgD.setCreated(now);
    orgD.setType(DatasetType.CHECKLIST);

    Dataset orgD2 = new Dataset();
    orgD2.setKey(UUID.randomUUID());
    orgD2.setTitle("orgD2");
    orgD2.setCreated(new Date(now.getTime() - 100000));
    orgD2.setType(DatasetType.CHECKLIST);

    resp3.setCount(2l);
    resp3.getResults().add(orgD);
    resp3.getResults().add(orgD2);
    when(os.publishedDatasets(Matchers.<UUID>any(), any(PagingRequest.class))).thenReturn(respEmpty);
    when(os.publishedDatasets(AdditionalMatchers.not(eq(org1.getKey())), any(PagingRequest.class))).thenReturn(resp3);

    is = Mockito.mock(InstallationService.class);
    Installation ins1 = new Installation();
    org1.setKey(INS_KEY);
    org1.setTitle("Ins1");
    when(is.get(eq(INS_KEY))).thenReturn(ins1);
    when(is.getHostedDatasets(Matchers.<UUID>any(), any(PagingRequest.class))).thenReturn(respEmpty);
    when(is.getHostedDatasets(AdditionalMatchers.not(eq(ins1.getKey())), any(PagingRequest.class))).thenReturn(resp3);

    // use default prod API
    NubConfiguration cfg = new NubConfiguration();
    cfg.neo = neoRepo.cfg;
    cfg.neoSources = neoRepo.cfg;
    cfg.clb.serverName = "localhost:" + database.getConnectionInfo().getPort();
    cfg.clb.databaseName = database.getConnectionInfo().getDbName();
    cfg.clb.user = database.getConnectionInfo().getUser();
    cfg.clb.password = "";
    cfg.sources.add(new NubSourceConfig(UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4"), Rank.PHYLUM));
    cfg.sources.add(new NubSourceConfig(UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d01984")));

    src = new ClbSourceList(ds, os, is, cfg);
  }

  /**
   * Test reading the source list
   */
  @Test
  public void testListSources() throws Exception {
    List<NubSource> sources = Iterables.asList(src);
    assertEquals(3, sources.size());
    assertEquals(Rank.PHYLUM, sources.get(0).ignoreRanksAbove);
    assertEquals(Rank.FAMILY, sources.get(1).ignoreRanksAbove);
    assertEquals(Rank.FAMILY, sources.get(2).ignoreRanksAbove);
    assertEquals(CHECKLIST_KEY, sources.get(0).key);
    assertEquals(oldDKey, sources.get(1).key);
  }

}