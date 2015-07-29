package org.gbif.checklistbank.nub.source;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.ClbConfiguration;
import org.gbif.checklistbank.nub.model.NubTags;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class ClbUsageSourceTest {

    private static final UUID CHECKLIST_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");
    private static ClbConfiguration cfg = new ClbConfiguration();
    private ClbUsageSource src;
    private DatasetService ds;
    private OrganizationService os;

    @Rule
    public DatabaseDrivenChecklistBankTestRule<NameUsageService> ddt = DatabaseDrivenChecklistBankTestRule.squirrels(NameUsageService.class);
    private UUID oldDKey;

    @Before
    public void init() {
        ds = Mockito.mock(DatasetService.class);
        Dataset squirrel = new Dataset();
        squirrel.setKey(CHECKLIST_KEY);
        squirrel.setTitle("Squirrels");
        squirrel.addMachineTag(new MachineTag(NubTags.NAMESPACE, NubTags.PRIORITY.tag, "10"));
        squirrel.addMachineTag(new MachineTag(NubTags.NAMESPACE, NubTags.RANK_LIMIT.tag, "KINGDOM"));
        PagingResponse<Dataset> resp = new PagingResponse<Dataset>();
        resp.setCount(1l);
        resp.getResults().add(squirrel);
        when(ds.listByType(any(DatasetType.class), any(PagingRequest.class))).thenReturn(resp);

        os = Mockito.mock(OrganizationService.class);
        Organization org1 = new Organization();
        org1.setKey(UUID.randomUUID());
        org1.setTitle("Org1");
        org1.addMachineTag(new MachineTag(NubTags.NAMESPACE, NubTags.PRIORITY.tag, "100"));
        org1.addMachineTag(new MachineTag(NubTags.NAMESPACE, NubTags.RANK_LIMIT.tag, "GENUS"));
        PagingResponse<Organization> resp2 = new PagingResponse<Organization>();
        resp2.setCount(1l);
        resp2.getResults().add(org1);
        when(os.list(any(PagingRequest.class))).thenReturn(resp2);

        oldDKey = UUID.randomUUID();
        final Date now = new Date();
        PagingResponse<Dataset> resp3 = new PagingResponse<Dataset>();
        Dataset orgD = new Dataset();
        orgD.setKey(oldDKey);
        orgD.setTitle("orgD");
        orgD.setCreated(new Date(now.getTime()-100000));
        Dataset orgD2 = new Dataset();
        orgD2.setKey(UUID.randomUUID());
        orgD2.setTitle("orgD2");
        orgD2.setCreated(now);
        resp3.setCount(2l);
        resp3.getResults().add(orgD);
        resp3.getResults().add(orgD2);
        when(os.publishedDatasets(eq(org1.getKey()), any(PagingRequest.class))).thenReturn(resp3);

        // use default prod API
        Properties props = ddt.getProperties();
        cfg.databaseName = props.getProperty("checklistbank.db.dataSource.databaseName");
        cfg.serverName = props.getProperty("checklistbank.db.dataSource.serverName");
        cfg.user = props.getProperty("checklistbank.db.dataSource.user");
        cfg.password = props.getProperty("checklistbank.db.dataSource.password");
        src = new ClbUsageSource(ds, os, cfg);
    }

    /**
     * integration test with prod registry
     */
    @Test
    public void testListSources() throws Exception {
        List<NubSource> sources = src.listSources();
        assertEquals(3, sources.size());
        assertEquals(10, sources.get(0).priority);
        assertEquals(100, sources.get(1).priority);
        assertEquals(100, sources.get(2).priority);
        assertEquals(Rank.KINGDOM, sources.get(0).ignoreRanksAbove);
        assertEquals(Rank.GENUS, sources.get(1).ignoreRanksAbove);
        assertEquals(Rank.GENUS, sources.get(2).ignoreRanksAbove);
        assertEquals(oldDKey, sources.get(2).key);
        assertNotEquals(oldDKey, sources.get(1).key);
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