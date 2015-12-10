package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.RelType;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;

import static org.junit.Assert.assertEquals;

public class TraversalsTest {
  private File dbf;
  private GraphDatabaseService db;
  private Node root;
  private Node child1;
  private Node child2;
  private Node child1Syn;
  private Node child2Syn;
  private Node bas;

  @Before
  public void init() throws IOException {
    dbf = Files.newTemporaryFolder();
    db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbf).newGraphDatabase();
  }

  @After
  public void cleanup() {
    db.shutdown();
    FileUtils.deleteQuietly(dbf);
  }

  private void setupNodesAndRels() {
    // SETUP NODES AND RELATIONS
    try (Transaction tx = db.beginTx()) {
      // single root
      root = db.createNode(Labels.ROOT);
      // chain up 10 children
      Node p = root;
      for (int idx = 1; idx <= 10; idx++) {
        Node child = db.createNode(Labels.TAXON);
        p.createRelationshipTo(child, RelType.PARENT_OF);
        p = child;
      }
      child1 = p;

      // 5 other nodes downward from root
      p = root;
      for (int idx = 1; idx <= 5; idx++) {
        Node child = db.createNode(Labels.TAXON);
        p.createRelationshipTo(child, RelType.PARENT_OF);
        p = child;
      }
      child2 = p;

      // synonyms
      child2Syn = db.createNode(Labels.SYNONYM);
      child2Syn.createRelationshipTo(child2, RelType.SYNONYM_OF);

      child1Syn = db.createNode(Labels.SYNONYM);
      child1Syn.createRelationshipTo(child1, RelType.SYNONYM_OF);
      child1Syn.createRelationshipTo(child2, RelType.PROPARTE_SYNONYM_OF);

      // one basionym with 3 rels
      Node n = db.createNode(Labels.TAXON);
      root.createRelationshipTo(n, RelType.PARENT_OF);

      bas = db.createNode(Labels.SYNONYM);
      bas.createRelationshipTo(n, RelType.SYNONYM_OF);
      bas.createRelationshipTo(n, RelType.BASIONYM_OF);
      bas.createRelationshipTo(child2, RelType.BASIONYM_OF);
      bas.createRelationshipTo(child2Syn, RelType.BASIONYM_OF);

      tx.success();
    }
  }

  @Test
  public void testTraversals() {
    setupNodesAndRels();

    try (Transaction tx = db.beginTx()) {
      assertTraversalSizes(Traversals.PARENT, 1, 0, 1, 0, 0, 0);
      assertTraversalSizes(Traversals.PARENTS, 10, 0, 5, 0, 0, 0);
      assertTraversalSizes(Traversals.CHILDREN, 0, 0, 0, 0, 3, 0);
      // descendants are synonyms (incl pro parte) and children
      assertTraversalSizes(Traversals.DESCENDANTS, 1, 0, 2, 0, 20, 0);
      assertTraversalSizes(Traversals.SYNONYMS, 1, 0, 1, 0, 0, 0);
      assertTraversalSizes(Traversals.ACCEPTED, 0, 2, 0, 1, 0, 1);
      assertTraversalSizes(Traversals.BASIONYM_GROUP, 1, 1, 4, 4, 1, 4);

      // numbers just as descendants +1 for the start node
      assertTraversalSizes(Traversals.SORTED_TREE, 2, 1, 3, 1, 21, 1);
      assertTraversalSizes(Traversals.SORTED_ACCEPTED_TREE, 1, 0, 1, 0, 17, 0);
    }
  }

  private void assertTraversalSizes(TraversalDescription td, int child1, int child1Syn, int child2, int child2Syn, int root, int bas) {
    assertEquals("child1 traversal wrong", child1, IteratorUtil.asCollection(td.traverse(this.child1)).size());
    assertEquals("child1Syn traversal wrong", child1Syn, IteratorUtil.asCollection(td.traverse(this.child1Syn)).size());
    assertEquals("child2 traversal wrong", child2, IteratorUtil.asCollection(td.traverse(this.child2)).size());
    assertEquals("child2Syn traversal wrong", child2Syn, IteratorUtil.asCollection(td.traverse(this.child2Syn)).size());
    assertEquals("root traversal wrong", root, IteratorUtil.asCollection(td.traverse(this.root)).size());
    assertEquals("bas traversal wrong", bas, IteratorUtil.asCollection(td.traverse(this.bas)).size());

  }

}