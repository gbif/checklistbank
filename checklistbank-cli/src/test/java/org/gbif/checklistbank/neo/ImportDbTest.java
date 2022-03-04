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

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.model.RankedName;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ImportDbTest {

  private UsageDao dao;
  private ImportDb idb;

  @Before
  public void setup() throws IOException {
    dao = UsageDao.temporaryDao(10);
    idb = new ImportDb(UUID.randomUUID(), dao);
  }

  private Node create(String taxonID, String canonical, String sciname) {
    NameUsage u = new NameUsage();
    u.setTaxonID(taxonID);
    u.setCanonicalName(canonical);
    u.setScientificName(sciname);
    return create(u);
  }

  private Node create(NameUsage u) {
    Node n = dao.createTaxon();
    dao.store(n.getId(), u, true);
    return n;
  }


  @Test
  public void testNodeByTaxonId() throws Exception {
    try(Transaction tx = dao.beginTx()) {
      assertNull(idb.nodeByTaxonId("312"));

      Node n = create("312", "Abies", "Abies Mill.");
      tx.success();

      assertEquals(n, idb.nodeByTaxonId("312"));
      assertNull(idb.nodeByTaxonId("412"));
    }
  }

  @Test
  public void testNodeByCanonical() throws Exception {
    try(Transaction tx = dao.beginTx()) {
      assertNull(idb.nodeByCanonical("Abies"));

      Node n = create("312", "Abies", "Abies Mill.");
      tx.success();

      assertEquals(n, idb.nodeByCanonical("Abies"));
      //assertEquals(node, dao.nodeByCanonical("abies"));
      assertNull(idb.nodeByCanonical("Abiess"));
    }
  }

  @Test
  public void testNodesByCanonical() throws Exception {
    try(Transaction tx = dao.beginTx()) {
      assertEquals(0, idb.nodesByCanonical("Abies").size());

      create("312", "Abies", "Abies Mill.");
      tx.success();
      assertEquals(1, idb.nodesByCanonical("Abies").size());

      create("313", "Abies", "Abies Mill.");
      tx.success();
      assertEquals(2, idb.nodesByCanonical("Abies").size());
    }
  }

  @Test
  public void testNodeBySciname() throws Exception {
    try(Transaction tx = dao.beginTx()) {
      assertNull(idb.nodeBySciname("Abies Mill."));

      Node n = create("312", "Abies", "Abies Mill.");
      tx.success();

      assertEquals(n, idb.nodeBySciname("Abies Mill."));
      assertNull(idb.nodeBySciname("Abies"));
    }
  }

  @Test
  public void testCreateTaxon() throws Exception {
    try(Transaction tx = dao.beginTx()) {
      assertNull(idb.nodeBySciname("Abies Mill."));

      Node n = idb.create(Origin.DENORMED_CLASSIFICATION, "Abies Mill.", Rank.GENUS, TaxonomicStatus.ACCEPTED, false).node;
      tx.success();

      assertEquals(n, idb.nodeBySciname("Abies Mill."));
      assertNull(idb.nodeBySciname("Abies"));
    }
  }

  @Test
  public void testHighestParent() throws Exception {
    try(Transaction tx = dao.beginTx()) {

      Node n = idb.create(Origin.DENORMED_CLASSIFICATION, "Abies Mill.", Rank.GENUS, TaxonomicStatus.ACCEPTED, false).node;
      tx.success();

      assertEquals(n, idb.getDirectParent(n).node);


      Node syn = idb.create(Origin.DENORMED_CLASSIFICATION, "Pinus", Rank.GENUS, TaxonomicStatus.SYNONYM, false).node;
      Node n2 = idb.create(Origin.DENORMED_CLASSIFICATION, "Pinaceae", Rank.FAMILY, TaxonomicStatus.ACCEPTED, false).node;
      Node n3 = idb.create(Origin.DENORMED_CLASSIFICATION, "Pinales", Rank.ORDER, TaxonomicStatus.ACCEPTED, false).node;
      Node n4 = idb.create(Origin.DENORMED_CLASSIFICATION, "Plantae", Rank.KINGDOM, TaxonomicStatus.ACCEPTED, false).node;
      n4.createRelationshipTo(n3, RelType.PARENT_OF);
      n3.createRelationshipTo(n2, RelType.PARENT_OF);
      n2.createRelationshipTo(n, RelType.PARENT_OF);
      syn.createRelationshipTo(n, RelType.SYNONYM_OF);
      tx.success();

      assertEquals(n4, idb.getDirectParent(n).node);
    }
  }

  @Test
  public void testMatchesClassification() throws Exception {
    try(Transaction tx = dao.beginTx()) {

      Node n = idb.create(Origin.DENORMED_CLASSIFICATION, "Abies Mill.", Rank.GENUS, TaxonomicStatus.ACCEPTED, false).node;
      Node syn = idb.create(Origin.DENORMED_CLASSIFICATION, "Pinus", Rank.GENUS, TaxonomicStatus.SYNONYM, false).node;
      Node n2 = idb.create(Origin.DENORMED_CLASSIFICATION, "Pinaceae", Rank.FAMILY, TaxonomicStatus.ACCEPTED, false).node;
      Node n3 = idb.create(Origin.DENORMED_CLASSIFICATION, "Pinales", Rank.ORDER, TaxonomicStatus.ACCEPTED, false).node;
      Node n4 = idb.create(Origin.DENORMED_CLASSIFICATION, "Plantae", Rank.KINGDOM, TaxonomicStatus.ACCEPTED, false).node;
      n4.createRelationshipTo(n3, RelType.PARENT_OF);
      n3.createRelationshipTo(n2, RelType.PARENT_OF);
      n2.createRelationshipTo(n, RelType.PARENT_OF);
      syn.createRelationshipTo(n, RelType.SYNONYM_OF);
      tx.success();

      List<RankedName> classification = Lists.newArrayList();
      assertTrue(idb.matchesClassification(n4, classification));
      assertFalse(idb.matchesClassification(n3, classification));
      assertFalse(idb.matchesClassification(n2, classification));
      assertFalse(idb.matchesClassification(n, classification));
//      assertFalse(idb.matchesClassification(syn, classification));

      classification.add(new RankedName("Plantae", Rank.KINGDOM));
      assertFalse(idb.matchesClassification(n4, classification));
      assertTrue(idb.matchesClassification(n3, classification));
      assertFalse(idb.matchesClassification(n2, classification));
      assertFalse(idb.matchesClassification(n, classification));

      classification.add(0, new RankedName("Pinales", Rank.ORDER));
      assertFalse(idb.matchesClassification(n4, classification));
      assertFalse(idb.matchesClassification(n3, classification));
      assertTrue(idb.matchesClassification(n2, classification));
      assertFalse(idb.matchesClassification(n, classification));

      classification.add(0, new RankedName("Pinaceae", Rank.SUBFAMILY));
      assertFalse(idb.matchesClassification(n4, classification));
      assertFalse(idb.matchesClassification(n3, classification));
      assertFalse(idb.matchesClassification(n2, classification));
      assertFalse(idb.matchesClassification(n, classification));
    }
  }


}