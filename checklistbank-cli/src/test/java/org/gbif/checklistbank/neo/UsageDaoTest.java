package org.gbif.checklistbank.neo;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.model.GraphFormat;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.source.ClasspathSource;
import org.gbif.utils.file.FileUtils;
import org.gbif.utils.text.StringUtils;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.yammer.metrics.MetricRegistry;
import org.assertj.core.util.Preconditions;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;

public class UsageDaoTest {
  private final static Random RND = new Random();
  UsageDao dao;

  @After
  public void destroy() {
    if (dao != null) {
      dao.closeAndDelete();
    }
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
      NameUsage u3 = usage(300, Rank.SPECIES);
      Node n3 = dao.create(u3);
      assertEquals(u3, dao.readUsage(n3, false));
      // expect previous data to remain
      verifyData(true, dao.getNeo().getNodeById(0), dao.getNeo().getNodeById(1));
    }
  }

  @Test
  @Ignore("manual test to generate GML test files for rod pages forest diff tool")
  public void testTrees2() throws Exception {
    try (ClasspathSource src = new ClasspathSource(41);) {
      src.init(true, false, false, false);
      dao = src.getDao();

      // add pro parte & basionym rel
      try (Transaction tx = dao.beginTx()) {
        Node ppsyn = dao.findByNameSingle("Acromantis javana");
        Node acc2 = dao.findByNameSingle("Acromantis montana");
        ppsyn.createRelationshipTo(acc2, RelType.PROPARTE_SYNONYM_OF);
        // basionym
        ppsyn.createRelationshipTo(acc2, RelType.BASIONYM_OF);
        ppsyn.addLabel(Labels.BASIONYM);

        tx.success();
      }

      Writer writer = FileUtils.startNewUtf8File(new File("/Users/markus/Desktop/test.txt"));
      try (Transaction tx = dao.beginTx()) {
        dao.printTree(writer, GraphFormat.LIST, true, Rank.SUBGENUS, null);
      }
      writer.flush();
    }
  }

  @Test
  public void testTrees() throws Exception {
    try (ClasspathSource src = new ClasspathSource(41);) {
      src.init(true, false, false, false);
      dao = src.getDao();

      // add pro parte & basionym rel
      try (Transaction tx = dao.beginTx()) {
        Node ppsyn = dao.findByNameSingle("Acromantis javana");
        Node acc2 = dao.findByNameSingle("Acromantis montana");
        ppsyn.createRelationshipTo(acc2, RelType.PROPARTE_SYNONYM_OF);
        // basionym
        ppsyn.createRelationshipTo(acc2, RelType.BASIONYM_OF);
        ppsyn.addLabel(Labels.BASIONYM);

        tx.success();
      }

      List<Rank> ranks = Lists.newArrayList((Rank) null);
      ranks.addAll(Rank.LINNEAN_RANKS);

      Writer writer = new PrintWriter(System.out);
      for (GraphFormat format : GraphFormat.values()) {
        for (int bool = 1; bool > 0; bool--) {
          for (Rank rank : ranks) {
            try (Transaction tx = dao.beginTx()) {
              writer.write("\n" + org.apache.commons.lang3.StringUtils.repeat("+", 60) + "\n");
              writer.write("Format=" + format + ", rank=" + rank + ", fullNames=" + (bool == 1) + "\n");
              writer.write(org.apache.commons.lang3.StringUtils.repeat("+", 80) + "\n");
              dao.printTree(writer, format, bool == 1, rank, null);
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
    final NameUsage u2 = usage(200, Rank.SPECIES);
    assertEquals(usage(112, Rank.GENUS), Preconditions.checkNotNull(dao.readUsage(n1, false), "Usage 1 missing"));
    assertEquals(usage(112, Rank.GENUS), Preconditions.checkNotNull(dao.readUsage(n1, true), "Usage 1 missing"));
    assertEquals(u2, Preconditions.checkNotNull(dao.readUsage(n2, false), "Usage 2 missing"));
    if (expectRels) {
      u2.setParentKey((int) n1.getId());
      // we want the canonical as the parent name!
      u2.setParent("Abies alba");
      u2.setBasionymKey((int) n1.getId());
      u2.setBasionym("Abies alba Mill.");
    }
    assertEquals(u2, Preconditions.checkNotNull(dao.readUsage(n2, true), "Usage 2 missing"));
  }

  private void testDao() throws Exception {
    try (Transaction tx = dao.beginTx()) {
      Node n1 = dao.create(usage(112, Rank.GENUS));
      Node n2 = dao.create(usage(200, Rank.SPECIES));

      verifyData(false, n1, n2);

      // now relate the 2 nodes and make sure when we read the relations the instance is changed accordingly
      n1.createRelationshipTo(n2, RelType.PARENT_OF);
      n1.createRelationshipTo(n2, RelType.BASIONYM_OF);
      verifyData(true, n1, n2);
      tx.success();
    }
  }

  public static NameUsage usage(int key) {
    return usage(key, Rank.SPECIES);
  }

  public static NameUsage usage(int key, Rank rank) {
    NameUsage u = new NameUsage();
    u.setKey(key);
    u.setKingdomKey(key);
    u.setParentKey(key);
    u.setAcceptedKey(key);
    u.setScientificName("Abies alba Mill.");
    u.setCanonicalName("Abies alba");
    u.setRank(rank);
    u.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);
    return u;
  }

  public static NubUsage nubusage(int key) {
    NubUsage u = new NubUsage();
    u.usageKey = key;
    u.datasetKey = UUID.randomUUID();
    u.kingdom = Kingdom.byNubUsageId(RND.nextInt(8));
    u.origin = Origin.SOURCE;
    u.addRemark(StringUtils.randomSpecies());
    u.parsedName = new ParsedName();
    u.parsedName.setGenusOrAbove(StringUtils.randomGenus());
    u.parsedName.setSpecificEpithet(StringUtils.randomEpithet());
    if (key % 5 == 0) {
      u.rank = Rank.SUBSPECIES;
      u.parsedName.setInfraSpecificEpithet(StringUtils.randomEpithet());
    } else {
      u.rank = Rank.SPECIES;
    }
    u.parsedName.setAuthorship(StringUtils.randomAuthor());
    u.parsedName.setYear(StringUtils.randomSpeciesYear());
    u.parsedName.setScientificName(u.parsedName.fullName());
    u.status = TaxonomicStatus.ACCEPTED;
    return u;
  }
}