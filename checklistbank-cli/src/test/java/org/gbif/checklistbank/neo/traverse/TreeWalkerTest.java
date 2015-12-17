package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.printer.TxtPrinter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import com.google.common.io.Resources;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class TreeWalkerTest {
  private File dbf;
  private GraphDatabaseService db;
  private Node root;
  private Node phylum;
  private Node order;
  private Node family;
  private Node genus;
  private Node bas;
  private Node ppSyn;
  private int idx;

  @Before
  public void init() throws IOException {
    dbf = Files.newTemporaryFolder();
    db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbf).newGraphDatabase();
    setupNodesAndRels();
  }

  @After
  public void cleanup() {
    db.shutdown();
    FileUtils.deleteQuietly(dbf);
  }

  private Node createNode(Rank rank, Labels ... label) {
    Node n = db.createNode(label);
    n.setProperty(NeoProperties.RANK, rank.ordinal());
    n.setProperty(NeoProperties.SCIENTIFIC_NAME, rank.name().toLowerCase() + idx++);
    return n;
  }

  private void setupNodesAndRels() {
    // SETUP NODES AND RELATIONS
    try (Transaction tx = db.beginTx()) {
      // single root
      root = createNode(Rank.KINGDOM, Labels.ROOT);
      // chain up phylum child
      phylum = createNode(Rank.PHYLUM, Labels.TAXON);
      root.createRelationshipTo(phylum, RelType.PARENT_OF);

      // phylum synonym
      Node syn = createNode(Rank.PHYLUM, Labels.TAXON, Labels.SYNONYM);
      syn.createRelationshipTo(phylum, RelType.SYNONYM_OF);

      // phylum children of all ranks
      Node p = phylum;
      for (Rank r : Rank.values()) {
        if (Rank.PHYLUM.higherThan(r) && r.higherThan(Rank.SUBGENUS)) {
          Node child = createNode(r, Labels.TAXON);
          p.createRelationshipTo(child, RelType.PARENT_OF);
          p = child;
          if (r == Rank.GENUS) {
            genus = child;
          } else if (r == Rank.FAMILY) {
            family = child;
          } else if (r == Rank.ORDER) {
            order = child;
          }
        }
      }

      // chain up more orders, each with 3 families
      for (int idx = 1; idx <= 3; idx++) {
        Node n = createNode(Rank.ORDER, Labels.TAXON);
        phylum.createRelationshipTo(n, RelType.PARENT_OF);
        for (int idx2 = 1; idx2 <= 3; idx2++) {
          Node f = createNode(Rank.FAMILY, Labels.TAXON);
          n.createRelationshipTo(f, RelType.PARENT_OF);
        }
      }


      // 3 species inside genus without a synonym
      for (int idx = 1; idx <= 3; idx++) {
        Node sp = createNode(Rank.SPECIES, Labels.TAXON);
        genus.createRelationshipTo(sp, RelType.PARENT_OF);
      }

      // 1 basionym
      bas = createNode(Rank.SPECIES, Labels.TAXON, Labels.BASIONYM);
      genus.createRelationshipTo(bas, RelType.PARENT_OF);

      // 5 more species inside genus
      for (int idx = 1; idx <= 5; idx++) {
        Node sp = createNode(Rank.SPECIES, Labels.TAXON);
        genus.createRelationshipTo(sp, RelType.PARENT_OF);

        Node spSyn = createNode(Rank.SPECIES, Labels.TAXON, Labels.SYNONYM);
        spSyn.createRelationshipTo(sp, RelType.SYNONYM_OF);
        bas.createRelationshipTo(spSyn, RelType.BASIONYM_OF);

        if (idx % 3 == 0) {
          // multiple synonyms and a basionym
          for (int idx2 = 1; idx2 <= 6; idx2++) {
            Node syn2 = createNode(idx2%2==0 ? Rank.SPECIES : Rank.SUBSPECIES, Labels.TAXON, Labels.SYNONYM);
            syn2.createRelationshipTo(sp, RelType.SYNONYM_OF);
          }
          bas = createNode(Rank.SPECIES, Labels.TAXON, Labels.BASIONYM, Labels.SYNONYM);
          bas.createRelationshipTo(sp, RelType.SYNONYM_OF);
          bas.createRelationshipTo(sp, RelType.BASIONYM_OF);
        }
      }

      // new genus with 4 species and a pro parte synonym for 3 of them
      Node genus2 = createNode(Rank.GENUS, Labels.TAXON);
      family.createRelationshipTo(genus2, RelType.PARENT_OF);

      ppSyn = createNode(Rank.SPECIES, Labels.TAXON, Labels.SYNONYM);
      for (int idx = 1; idx <= 4; idx++) {
        Node sp = createNode(Rank.SPECIES, Labels.TAXON);
        genus2.createRelationshipTo(sp, RelType.PARENT_OF);

        if (idx == 2) {
          ppSyn.createRelationshipTo(sp, RelType.SYNONYM_OF);
        }else if (idx > 2) {
          ppSyn.createRelationshipTo(sp, RelType.PROPARTE_SYNONYM_OF);
        }
      }

      tx.success();
    }
  }

  @Test
  public void testWalkTree() throws Exception {
    StringWriter writer = new StringWriter();
    TreeWalker.walkTree(db, new TxtPrinter(writer));
    System.out.println(writer.toString());
    assertEquals(Resources.toString(Resources.getResource("traverse/tree.txt"), Charsets.UTF_8), writer.toString());

    writer = new StringWriter();
    TreeWalker.walkTree(db, phylum, null, null, new TxtPrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/treePhylum.txt"), Charsets.UTF_8), writer.toString());

    writer = new StringWriter();
    TreeWalker.walkTree(db, genus, null, null, new TxtPrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/treeGenus.txt"), Charsets.UTF_8), writer.toString());

    writer = new StringWriter();
    TreeWalker.walkTree(db, bas, null, null, new TxtPrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/treeBasionym.txt"), Charsets.UTF_8), writer.toString());

    writer = new StringWriter();
    TreeWalker.walkTree(db, phylum, Rank.ORDER, null, new TxtPrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/tree-order.txt"), Charsets.UTF_8), writer.toString());
  }

  @Test
  public void testWalkAcceptedTree() throws Exception {
    StringWriter writer = new StringWriter();
    TreeWalker.walkAcceptedTree(db, new TxtPrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/treeAccepted.txt"), Charsets.UTF_8), writer.toString());

    writer = new StringWriter();
    TreeWalker.walkAcceptedTree(db, genus, null, null, new TxtPrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/treeAcceptedGenus.txt"), Charsets.UTF_8), writer.toString());
  }

  @Test
  public void testChunkingHandler() throws Exception {
    StringWriter writer = new StringWriter();
    TreeWalker.walkAcceptedTree(db, new TxtPrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/treeAccepted.txt"), Charsets.UTF_8), writer.toString());

    writer = new StringWriter();
    TreeWalker.walkAcceptedTree(db, genus, null, null, new TxtPrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/treeAcceptedGenus.txt"), Charsets.UTF_8), writer.toString());
  }

}