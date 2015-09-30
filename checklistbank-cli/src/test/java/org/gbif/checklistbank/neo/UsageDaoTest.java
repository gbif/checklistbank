package org.gbif.checklistbank.neo;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.MapDbObjectSerializerTest;

import java.util.UUID;

import com.google.common.io.Files;
import com.yammer.metrics.MetricRegistry;
import org.assertj.core.util.Preconditions;
import org.junit.After;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;

public class UsageDaoTest {

    UsageDao dao;

    @After
    public void destroy() {
        dao.closeAndDelete();
    }

    @Test
    public void tmpUsageDao() throws Exception {
        dao = UsageDao.temporaryDao(10);
        testDao();
    }

    @Test
    public void persistenUsageDao() throws Exception {
        NeoConfiguration cfg = new NeoConfiguration();
        cfg.neoRepository = Files.createTempDir();

        UUID uuid = UUID.randomUUID();
        MetricRegistry reg = new MetricRegistry("daotest");

        dao = UsageDao.persistentDao(cfg, uuid, false, reg, true);
        testDao();

        // close and reopen. Make sure data survived
        dao.close();
        dao = UsageDao.persistentDao(cfg, uuid, false, reg, false);
        try (Transaction tx = dao.beginTx()) {
            NameUsage u3 = MapDbObjectSerializerTest.usage(300, Rank.SPECIES);
            Node n3 = dao.create(u3);
            assertEquals(u3, dao.readUsage(n3, false));
            // expect previous data to remain
            verifyData(true, dao.getNeo().getNodeById(0), dao.getNeo().getNodeById(1));
        }
    }

    private void verifyData(boolean expectRels, Node n1, Node n2) throws Exception {
        assertEquals(MapDbObjectSerializerTest.usage(112, Rank.GENUS), Preconditions.checkNotNull(dao.readUsage(n1, false), "Usage 1 missing"));
        assertEquals(MapDbObjectSerializerTest.usage(112, Rank.GENUS), Preconditions.checkNotNull(dao.readUsage(n1, true), "Usage 1 missing"));
        assertEquals(MapDbObjectSerializerTest.usage(200, Rank.SPECIES), Preconditions.checkNotNull(dao.readUsage(n2, false), "Usage 2 missing"));
        NameUsage u2 = MapDbObjectSerializerTest.usage(200, Rank.SPECIES);
        if (expectRels) {
            u2.setParentKey((int)n1.getId());
            u2.setParent("Abies alba Mill.");
            u2.setBasionymKey((int) n1.getId());
            u2.setBasionym("Abies alba Mill.");
        }
        assertEquals(u2, Preconditions.checkNotNull(dao.readUsage(n2, true), "Usage 2 missing"));
    }

    private void testDao() throws Exception {
        try (Transaction tx = dao.beginTx()) {
            Node n1 = dao.create(MapDbObjectSerializerTest.usage(112, Rank.GENUS));
            Node n2 = dao.create(MapDbObjectSerializerTest.usage(200, Rank.SPECIES));

            verifyData(false, n1, n2);

            // now relate the 2 nodes and make sure when we read the relations the instance is changed accordingly
            n1.createRelationshipTo(n2, RelType.PARENT_OF);
            n1.createRelationshipTo(n2, RelType.BASIONYM_OF);
            verifyData(true, n1, n2);
            tx.success();
        }
    }
}