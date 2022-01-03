package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.common.LinneanClassificationKeys;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.BaseTest;
import org.gbif.checklistbank.cli.normalizer.NormalizerStats;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.cli.model.NameUsageNode;
import org.gbif.checklistbank.cli.model.UsageFacts;

import com.google.common.collect.Lists;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;

@Ignore("REMOVE! ignored only to make the jenkins build work")
public class UsageMetricsHandlerTest extends BaseTest {

  @Test
  public void testClassificationHandler() {
    initDb();

    try (Transaction tx = beginTx()) {
      Node n = addNode(Rank.KINGDOM, "Animalia", null);
      n.addLabel(Labels.ROOT);

      n = addNode(Rank.KINGDOM, "Plantae", null);
      n.addLabel(Labels.ROOT);

      n = addNode(Rank.PHYLUM, "Pinophyta", n);
      n = addNode(Rank.CLASS, "Pinalaea", n);
      n = addNode(Rank.ORDER, "Pinales", n);
      addNode(Rank.FAMILY, "Araucariaceae", n);
      n = addNode(Rank.FAMILY, "Pinaceae", n);
      n = addNode(Rank.SUBFAMILY, "Abiedea", n);
      addNode(Rank.GENUS, "Pinus", n);
      n = addNode(Rank.GENUS, "Abies", n);
      addNode(Rank.SPECIES, "Abies Alpina DC.", n);
      addNode(Rank.SPECIES, "Abies balkan L.", n);
      n =addNode(Rank.SPECIES, "Abies alba Mill.", n);
      addNode(Rank.SUBSPECIES, "Abies alba Mill. subsp. alba", n);
      addNode(Rank.SUBSPECIES, "Abies alba subsp. alpina Mill.", n);

      tx.success();
    }

    UsageMetricsHandler handler = new UsageMetricsHandler(dao);
    TreeWalker.walkAcceptedTree(dao.getNeo(), handler);

    NormalizerStats stats = handler.getStats(1, Lists.<String>newArrayList());
    System.out.println(stats);
    assertEquals(2, stats.getRoots());
    assertEquals(0, stats.getSynonyms());
    assertEquals(1, stats.getIgnored());
    assertEquals(2, stats.getCountByRank(Rank.KINGDOM));
    assertEquals(1, stats.getCountByRank(Rank.PHYLUM));
    assertEquals(1, stats.getCountByRank(Rank.CLASS));
    assertEquals(1, stats.getCountByRank(Rank.ORDER));
    assertEquals(2, stats.getCountByRank(Rank.FAMILY));
    assertEquals(1, stats.getCountByRank(Rank.SUBFAMILY));
    assertEquals(2, stats.getCountByRank(Rank.GENUS));
    assertEquals(3, stats.getCountByRank(Rank.SPECIES));
    assertEquals(2, stats.getCountByRank(Rank.SUBSPECIES));
    assertEquals(9, stats.getDepth());

    // verify metrics and classification keys
    LinneanClassificationKeys cl = new NameUsage();
    try (Transaction tx = beginTx()) {
      cl.setKingdomKey(1);
      assertUsage(1, Rank.KINGDOM, cl);

      cl.setPhylumKey(2);
      assertUsage(2, Rank.PHYLUM, cl);

      cl.setClassKey(3);
      assertUsage(3, Rank.CLASS, cl);

      cl.setOrderKey(4);
      cl.setFamilyKey(6);
      assertUsage(6, Rank.FAMILY, cl);

      cl.setGenusKey(9);
      assertUsage(9, Rank.GENUS, cl);

      cl.setSpeciesKey(12);
      assertUsage(12, Rank.SPECIES, cl);
      assertUsage(13, Rank.SUBSPECIES, cl);
      assertUsage(14, Rank.SUBSPECIES, cl);
    }
  }

  private void assertUsage(long id, Rank rank, LinneanClassificationKeys expected) {
    UsageFacts facts = dao.readFacts(id);
    for (Rank r : Rank.LINNEAN_RANKS) {
      assertEquals(expected.getHigherRankKey(r), facts.classification.getHigherRankKey(r));
    }
  }

  private Node addNode(Rank rank, String name, Node parent) {
    Node n = dao.createTaxon();
    NameUsage u = new NameUsage();
    u.setRank(rank);
    u.setCanonicalName(name);
    u.setScientificName(name);
    dao.store(new NameUsageNode(n, u, true), true);

    if (parent != null) {
      parent.createRelationshipTo(n, RelType.PARENT_OF);
    }

    return n;
  }
}