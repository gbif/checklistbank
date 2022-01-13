/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.neo;

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
import org.neo4j.helpers.collection.Iterables;

import static org.junit.Assert.assertEquals;

/**
 * Neo4j changes its behavior between 1.9, 2.0 and 2.2 version when it comes to the state of modified nodes and
 * relations during transactions. This test makes sure the behavior coded for in this project matches the currently
 * used neo4j version.
 *
 * In particular we expect the getAllNodes() method to iterate over all nodes but the ones created during the iteration
 * if the transaction was not yet committed.
 */
public class TransactionTest {

  private File dbf;
  private GraphDatabaseService db;

  @Before
  public void init() throws IOException {
    dbf = Files.newTemporaryFolder();
    db = new GraphDatabaseFactory().newEmbeddedDatabase(dbf);
  }

  @After
  public void cleanup() {
    db.shutdown();
    FileUtils.deleteQuietly(dbf);
  }

  @Test
  public void testTransactionsGetAll() {
    Transaction tx = db.beginTx();
    // initial 100 nodes without relations
    for (int idx=1; idx<=100; idx++) {
      db.createNode(Labels.ROOT);
    }
    tx.success();
    tx.close();

    assertEquals(100, countAll());

    // now iterate over those 100 nodes, persistent a new node and link them
    // expect those nodes not to be shown in the iterator which opened at the beginning of the tx.
    tx = db.beginTx();
    int counter = 0;
    for (Node n : db.getAllNodes()) {
      Node n2 = db.createNode(Labels.TAXON);
      n.createRelationshipTo(n2, RelType.PARENT_OF);
      counter++;
    }
    tx.success();
    tx.close();

    assertEquals(100, counter);
    assertEquals(200, countAll());

    // now commit in batches. We expect the newly created nodes to appear in the iterator as we reopen the tx in between ...
    tx = db.beginTx();
    counter = 0;
    for (Node n : db.getAllNodes()) {
      if (counter < 100) {
        Node n2 = db.createNode(Labels.SYNONYM);
        n2.createRelationshipTo(n, RelType.SYNONYM_OF);
      }
      counter++;
      if (counter % 10 == 0) {
        tx.success();
        tx.close();
        tx = db.beginTx();
      }
    }
    tx.success();
    tx.close();

    assertEquals(300, counter);
    assertEquals(300, countAll());


    // now commit at the end, but mark tx as success every time...
    tx = db.beginTx();
    counter = 0;
    for (Node n : db.getAllNodes()) {
      if (counter < 100) {
        Node n2 = db.createNode(Labels.SYNONYM);
        n2.createRelationshipTo(n, RelType.SYNONYM_OF);
      }
      counter++;
      tx.success();
    }
    tx.success();
    tx.close();

    assertEquals(300, counter);
    assertEquals(400, countAll());

    // now use outer and inner transactions.
    // When committing the inner transactions they dont affect the outer one
    tx = db.beginTx();
    counter = 0;
    for (Node n : db.getAllNodes()) {
      if (counter < 100) {
        Transaction tx2 = db.beginTx();
        Node n2 = db.createNode(Labels.SYNONYM);
        n2.createRelationshipTo(n, RelType.SYNONYM_OF);
        tx2.success();
        tx2.close();
      }
      counter++;
    }
    tx.success();
    tx.close();

    assertEquals(400, counter);
    assertEquals(500, countAll());

  }

  private long countAll() {
    try (Transaction tx = db.beginTx()) {
      return Iterables.count(db.getAllNodes());
    }
  }

}
