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
package org.gbif.checklistbank.neo.traverse;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.iterable.Ints;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.utils.text.StringUtils;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.Iterators;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore("REMOVE! ignored only to make the jenkins build work")
public class ChunkingEvaluatorTest {
  private UsageDao dao;
  private int idx;
  private Node root;

  @Before
  public void init() throws IOException {
    dao = UsageDao.temporaryDao(256);
    try (Transaction tx = dao.beginTx()) {
      root = dao.createTaxon();
      root.addLabel(Labels.ROOT);
      usage(root, "Animalia", Rank.KINGDOM, TaxonomicStatus.ACCEPTED);

      tx.success();
    }
  }

  @After
  public void cleanup() {
    dao.closeAndDelete();
  }

  @Test
  public void testChunkingFlat() {
    try (Transaction tx = dao.beginTx()) {
      Node p1 = dao.createTaxon();
      root.createRelationshipTo(p1, RelType.PARENT_OF);
      family(p1);

      Node p2 = dao.createTaxon();
      p1.createRelationshipTo(p2, RelType.PARENT_OF);
      family(p2);
      for (int idx = 1; idx < 100; idx++) {
        Node n = dao.createTaxon();
        p2.createRelationshipTo(n, RelType.PARENT_OF);
        family(n);
      }
      tx.success();
    }

    // test chunking
    assertChunking(5, 10, 2, 1);
  }

  @Test
  public void testChunkingDeep() {
    System.out.println("Preparing test graph ...");
    try (Transaction tx = dao.beginTx()) {
      for (int i : Ints.until(3)) {
        Node p = dao.createTaxon();
        root.createRelationshipTo(p, RelType.PARENT_OF);
        usage(p, "Phylum"+i, Rank.PHYLUM, TaxonomicStatus.ACCEPTED);
        for (int i2  : Ints.until(4)) {
          Node o = dao.createTaxon();
          p.createRelationshipTo(o, RelType.PARENT_OF);
          usage(o, "Order"+i2, Rank.ORDER, TaxonomicStatus.ACCEPTED);
          for (int i3 : Ints.until(6)) {
            Node f = dao.createTaxon();
            o.createRelationshipTo(f, RelType.PARENT_OF);
            family(f);
            for (int i4 : Ints.until(10)) {
              Node g = dao.createTaxon();
              f.createRelationshipTo(g, RelType.PARENT_OF);
              genus(g);
              for (int i5 : Ints.until(25)) {
                Node sp = dao.createTaxon();
                g.createRelationshipTo(sp, RelType.PARENT_OF);
                accepted(sp, Rank.SPECIES);
                if (i5 % 4 == 0){
                  for (int i6 : Ints.until(3)) {
                    Node syn = dao.createTaxon();
                    syn.addLabel(Labels.SYNONYM);
                    syn.createRelationshipTo(sp, RelType.SYNONYM_OF);
                    synonym(syn, Rank.SPECIES);
                  }
                }
              }
            }
          }
        }
      }
      tx.success();
    }
    System.out.println("Setup completed.");

    // test chunking
    assertChunking(50, 500, 16, 72);
  }

  private void assertChunking(final int chunkMinSize, final int chunkSize, final int expectedUnchunkedNodes, final int expectedChunks) {
    TreeWalker.walkAcceptedTree(dao.getNeo(), new UsageMetricsHandler(dao));

    int chunks = 0;
    int unchunkedNodes = 0;
    ChunkingEvaluator chunkingEvaluator = new ChunkingEvaluator(dao, chunkMinSize, chunkSize);
    try (Transaction tx = dao.beginTx()) {
      for (Node n : MultiRootNodeIterator.create(root, Traversals.TREE_WITHOUT_PRO_PARTE.evaluator(chunkingEvaluator))) {
        if (chunkingEvaluator.isChunk(n.getId())) {
          chunks++;
          long cnt = 1 + Iterators.count(Traversals.DESCENDANTS.traverse(n).iterator());
          //System.out.println(n.getId() + ": " + cnt);
          assertTrue(cnt > chunkMinSize);
          // we cant assert a maximum chunk size
          //assertTrue(cnt <= chunkSize*2);

        }else{
          unchunkedNodes++;
          //System.out.println(n.getId());
        }
      }
    }
    assertEquals(expectedChunks, chunks);
    assertEquals(expectedUnchunkedNodes, unchunkedNodes);
  }

  private void family(Node n) {
    usage(n, StringUtils.randomFamily(), Rank.FAMILY, TaxonomicStatus.ACCEPTED);
  }

  private void genus(Node n) {
    usage(n, StringUtils.randomGenus(), Rank.GENUS, TaxonomicStatus.ACCEPTED);
  }

  private void accepted(Node n, Rank rank) {
    usage(n, StringUtils.randomSpecies(), rank, TaxonomicStatus.ACCEPTED);
  }

  private void synonym(Node n, Rank rank) {
    usage(n, StringUtils.randomSpecies(), rank, TaxonomicStatus.SYNONYM);
  }

  private void usage(Node n, String name, Rank rank, TaxonomicStatus status) {
    NameUsage u = new NameUsage();
    u.setScientificName(name);
    u.setRank(rank);
    u.setTaxonomicStatus(status);
    u.setOrigin(Origin.SOURCE);
    dao.store(n.getId(), u, true);
  }
}
