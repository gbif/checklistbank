package org.gbif.checklistbank.nub.source;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.neo4j.helpers.collection.Iterables;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class ClbSourceListTest {

    private static final UUID CHECKLIST_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");
    private static final UUID ORG_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d01984");
    private static NubConfiguration cfg = new NubConfiguration();
    private ClbSourceList src;
    private DatasetService ds;
    private OrganizationService os;

    @Rule
    public ClbDbTestRule dbSetup = ClbDbTestRule.squirrels();

    private UUID oldDKey;

    @Before
    public void init() {
        ds = Mockito.mock(DatasetService.class);
        Dataset squirrel = new Dataset();
        squirrel.setKey(CHECKLIST_KEY);
        squirrel.setTitle("Squirrels");
        when(ds.get(eq(CHECKLIST_KEY))).thenReturn(squirrel);

        os = Mockito.mock(OrganizationService.class);
        Organization org1 = new Organization();
        org1.setKey(ORG_KEY);
        org1.setTitle("Org1");
        when(os.get(eq(ORG_KEY))).thenReturn(org1);

        oldDKey = UUID.randomUUID();
        final Date now = new Date();
        PagingResponse<Dataset> resp3 = new PagingResponse<Dataset>();

        Dataset orgD = new Dataset();
        orgD.setKey(oldDKey);
        orgD.setTitle("orgD");
        orgD.setCreated(now);

        Dataset orgD2 = new Dataset();
        orgD2.setKey(UUID.randomUUID());
        orgD2.setTitle("orgD2");
        orgD2.setCreated(new Date(now.getTime()-100000));

        resp3.setCount(2l);
        resp3.getResults().add(orgD);
        resp3.getResults().add(orgD2);
        when(os.publishedDatasets(eq(org1.getKey()), any(PagingRequest.class))).thenReturn(resp3);

        // use default prod API
        Properties props = dbSetup.getProperties();
        cfg.clb.databaseName = props.getProperty("checklistbank.db.dataSource.databaseName");
        cfg.clb.serverName = props.getProperty("checklistbank.db.dataSource.serverName");
        cfg.clb.user = props.getProperty("checklistbank.db.dataSource.user");
        cfg.clb.password = props.getProperty("checklistbank.db.dataSource.password");
        cfg.sourceList = URI.create("nub-test-sources.txt");
        src = new ClbSourceList(ds, os, cfg);
    }

    /**
     * Test reading the source list
     */
    @Test
    public void testListSources() throws Exception {
        List<NubSource> sources = Iterables.toList(src);
        assertEquals(3, sources.size());
        assertEquals(Rank.PHYLUM, sources.get(0).ignoreRanksAbove);
        assertEquals(Rank.FAMILY, sources.get(1).ignoreRanksAbove);
        assertEquals(Rank.FAMILY, sources.get(2).ignoreRanksAbove);
        assertEquals(CHECKLIST_KEY, sources.get(0).key);
        assertEquals(oldDKey, sources.get(1).key);
    }

}