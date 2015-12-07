package org.gbif.checklistbank.neo;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.MapDbObjectSerializerTest;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.show.GraphFormat;
import org.gbif.checklistbank.nub.source.ClasspathSource;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.UUID;

import com.google.common.base.Throwables;
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

  @Test
  public void testTrees() throws Exception {
    try (ClasspathSource src = new ClasspathSource(8);) {
      src.init(true, false);
      dao = src.open();

      Writer writer = new PrintWriter(System.out);
      for (Rank rank : Rank.LINNEAN_RANKS) {
        for (GraphFormat format : GraphFormat.values()) {
          for (int bool = 1; bool>0; bool--) {
            try (Transaction tx = dao.beginTx()) {
              writer.write("\n"+org.apache.commons.lang3.StringUtils.repeat("+", 60)+"\n");
              writer.write("Format="+format+", rank="+rank+", fullNames="+(bool==1)+"\n");
              writer.write(org.apache.commons.lang3.StringUtils.repeat("+", 80)+"\n");
              dao.printTree(writer, format, bool==1, rank);
            } catch (IllegalArgumentException e) {
              if (format != GraphFormat.GML && format != GraphFormat.TAB) {
                Throwables.propagate(e);
              }
            }
          }
        }
      }
      writer.flush();
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