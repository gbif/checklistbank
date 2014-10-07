package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.normalizer.NeoTest;
import org.gbif.checklistbank.neo.TaxonProperties;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.beust.jcommander.internal.Lists;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;

public class TaxonOrderTest extends NeoTest {
  @Test
  public void testCompare() throws Exception {
    initDb(UUID.randomUUID());
    try (Transaction tx = beginTx()) {
      TaxonOrder order = new TaxonOrder();
      List<Node> nodes = Lists.newArrayList(
        build(Rank.GENUS, "Pinus"),
        build(Rank.GENUS, "Abies"),
        build(Rank.FAMILY, "Pinaceae"),
        build(Rank.KINGDOM, "Plantae"),
        build(Rank.UNRANKED, "nonsense"),
        build(null, "Incertae sedis"),
        build(Rank.INFRAGENERIC_NAME, "Abieta")
      );
      Collections.sort(nodes, order);

      assertEquals(3, nodes.get(0).getId());
      assertEquals(2, nodes.get(1).getId());
      assertEquals(1, nodes.get(2).getId());
      assertEquals(0, nodes.get(3).getId());
    }
  }

  private Node build(Rank rank, String name){
    Node n = db.createNode();
    if (rank != null) {
      mapper.storeEnum(n, TaxonProperties.RANK, rank);
    }
    if (name != null) {
      n.setProperty(TaxonProperties.SCIENTIFIC_NAME, name);
    }
    return n;
  }
}