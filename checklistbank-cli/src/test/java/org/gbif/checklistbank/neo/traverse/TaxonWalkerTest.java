package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.printer.TreePrinter;

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
public class TaxonWalkerTest {
  private File dbf;
  private GraphDatabaseService db;
  private Node root;
  private Node phylum;
  private Node genus;
  private Node bas;
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

      // phylum children
      Node p = phylum;
      for (Rank r : Rank.values()) {
        if (Rank.PHYLUM.higherThan(r) && r.higherThan(Rank.SUBGENUS)) {
          Node child = createNode(r, Labels.TAXON);
          p.createRelationshipTo(child, RelType.PARENT_OF);
          p = child;
          if (r == Rank.GENUS) {
            genus = child;
          }
        }
      }

      // 1 basionym
      bas = createNode(Rank.SPECIES, Labels.TAXON, Labels.BASIONYM);
      genus.createRelationshipTo(bas, RelType.PARENT_OF);

      // 5 species inside genus
      for (int idx = 1; idx <= 5; idx++) {
        Node sp = createNode(Rank.SPECIES, Labels.TAXON);
        genus.createRelationshipTo(sp, RelType.PARENT_OF);

        Node spSyn = createNode(Rank.SPECIES, Labels.TAXON, Labels.SYNONYM);
        spSyn.createRelationshipTo(sp, RelType.SYNONYM_OF);
        bas.createRelationshipTo(spSyn, RelType.BASIONYM_OF);
      }

      tx.success();
    }
  }

  @Test
  public void testWalkTree() throws Exception {
    StringWriter writer = new StringWriter();
    TaxonWalker.walkTree(db, new TreePrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/tree.txt"), Charsets.UTF_8), writer.toString());

    writer = new StringWriter();
    TaxonWalker.walkTree(db, phylum, null, null, new TreePrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/treePhylum.txt"), Charsets.UTF_8), writer.toString());

    writer = new StringWriter();
    TaxonWalker.walkTree(db, genus, null, null, new TreePrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/treeGenus.txt"), Charsets.UTF_8), writer.toString());

    writer = new StringWriter();
    TaxonWalker.walkTree(db, bas, null, null, new TreePrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/treeBasionym.txt"), Charsets.UTF_8), writer.toString());

    writer = new StringWriter();
    TaxonWalker.walkTree(db, phylum, Rank.ORDER, null, new TreePrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/tree-f-order.txt"), Charsets.UTF_8), writer.toString());
  }

  @Test
  public void testWalkAcceptedTree() throws Exception {
    StringWriter writer = new StringWriter();
    TaxonWalker.walkAcceptedTree(db, new TreePrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/treeAccepted.txt"), Charsets.UTF_8), writer.toString());

    writer = new StringWriter();
    TaxonWalker.walkAcceptedTree(db, genus, null, null, new TreePrinter(writer));
    assertEquals(Resources.toString(Resources.getResource("traverse/treeAcceptedGenus.txt"), Charsets.UTF_8), writer.toString());
  }

}