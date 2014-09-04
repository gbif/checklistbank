package org.gbif.checklistbank.cli.common;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.normalizer.NeoTest;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.beust.jcommander.internal.Lists;
import com.yammer.metrics.MetricRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.*;

public class NeoRunnableTest {

  private NeoRunnable neo;
  private NeoConfiguration cfg;

  public static class NeoRunnableImpl extends NeoRunnable {

    public NeoRunnableImpl(NeoConfiguration cfg) {
      super(UUID.randomUUID(), cfg, new MetricRegistry("test"));
    }

    @Override
    public void run() {
      throw new UnsupportedOperationException("Not implemented yet");
    }
  }

  @Before
  public void setup() throws IOException {
    cfg = new NeoConfiguration();
    File tmp = FileUtils.createTempDir();
    cfg.neoRepository = tmp;
    neo = new NeoRunnableImpl(cfg);
    neo.setupDb();
    neo.setupIndices();
  }

  @After
  public void cleanup() throws Exception {
    neo.tearDownDb();
    org.apache.commons.io.FileUtils.cleanDirectory(cfg.neoRepository);
    cfg.neoRepository.delete();
  }

  private Node create(String taxonID, String canonical, String sciname) {
    NameUsage u = new NameUsage();
    u.setTaxonID(taxonID);
    u.setCanonicalName(canonical);
    u.setScientificName(sciname);
    return create(u);
  }

  private Node create(NameUsage u) {
    Node n = neo.db.createNode(Labels.TAXON);
    neo.mapper.store(n, u, false);
    return n;
  }


  @Test
  public void testNodeByTaxonId() throws Exception {
    try(Transaction tx = neo.db.beginTx()) {
      assertNull(neo.nodeByTaxonId("312"));

      Node n = create("312", "Abies", "Abies Mill.");
      tx.success();

      assertEquals(n, neo.nodeByTaxonId("312"));
      assertNull(neo.nodeByTaxonId("412"));
    }
  }

  @Test
  public void testNodeByCanonical() throws Exception {
    try(Transaction tx = neo.db.beginTx()) {
      assertNull(neo.nodeByCanonical("Abies"));

      Node n = create("312", "Abies", "Abies Mill.");
      tx.success();

      assertEquals(n, neo.nodeByCanonical("Abies"));
      //assertEquals(n, neo.nodeByCanonical("abies"));
      assertNull(neo.nodeByCanonical("Abiess"));
    }
  }

  @Test
  public void testNodesByCanonical() throws Exception {
    try(Transaction tx = neo.db.beginTx()) {
      assertEquals(0, neo.nodesByCanonical("Abies").size());

      create("312", "Abies", "Abies Mill.");
      tx.success();
      assertEquals(1, neo.nodesByCanonical("Abies").size());

      create("313", "Abies", "Abies Mill.");
      tx.success();
      assertEquals(2, neo.nodesByCanonical("Abies").size());
    }
  }

  @Test
  public void testNodeBySciname() throws Exception {
    try(Transaction tx = neo.db.beginTx()) {
      assertNull(neo.nodeBySciname("Abies Mill."));

      Node n = create("312", "Abies", "Abies Mill.");
      tx.success();

      assertEquals(n, neo.nodeBySciname("Abies Mill."));
      assertNull(neo.nodeBySciname("Abies"));
    }
  }

  @Test
  public void testCreateTaxon() throws Exception {
    try(Transaction tx = neo.db.beginTx()) {
      assertNull(neo.nodeBySciname("Abies Mill."));

      Node n = neo.create(Origin.DENORMED_CLASSIFICATION, "Abies Mill.", Rank.GENUS, TaxonomicStatus.ACCEPTED);
      tx.success();

      assertEquals(n, neo.nodeBySciname("Abies Mill."));
      assertNull(neo.nodeBySciname("Abies"));
    }
  }

  @Test
  public void testHighestParent() throws Exception {
    try(Transaction tx = neo.db.beginTx()) {

      Node n = neo.create(Origin.DENORMED_CLASSIFICATION, "Abies Mill.", Rank.GENUS, TaxonomicStatus.ACCEPTED);
      tx.success();

      assertEquals(n, neo.getHighestParent(n).node);


      Node syn = neo.create(Origin.DENORMED_CLASSIFICATION, "Pinus", Rank.GENUS, TaxonomicStatus.SYNONYM);
      Node n2 = neo.create(Origin.DENORMED_CLASSIFICATION, "Pinaceae", Rank.FAMILY, TaxonomicStatus.ACCEPTED);
      Node n3 = neo.create(Origin.DENORMED_CLASSIFICATION, "Pinales", Rank.ORDER, TaxonomicStatus.ACCEPTED);
      Node n4 = neo.create(Origin.DENORMED_CLASSIFICATION, "Plantae", Rank.KINGDOM, TaxonomicStatus.ACCEPTED);
      n4.createRelationshipTo(n3, RelType.PARENT_OF);
      n3.createRelationshipTo(n2, RelType.PARENT_OF);
      n2.createRelationshipTo(n, RelType.PARENT_OF);
      syn.createRelationshipTo(n, RelType.SYNONYM_OF);
      tx.success();

      assertEquals(n4, neo.getHighestParent(n).node);
    }
  }

  @Test
  public void testMatchesClassification() throws Exception {
    try(Transaction tx = neo.db.beginTx()) {

      Node n = neo.create(Origin.DENORMED_CLASSIFICATION, "Abies Mill.", Rank.GENUS, TaxonomicStatus.ACCEPTED);
      Node syn = neo.create(Origin.DENORMED_CLASSIFICATION, "Pinus", Rank.GENUS, TaxonomicStatus.SYNONYM);
      Node n2 = neo.create(Origin.DENORMED_CLASSIFICATION, "Pinaceae", Rank.FAMILY, TaxonomicStatus.ACCEPTED);
      Node n3 = neo.create(Origin.DENORMED_CLASSIFICATION, "Pinales", Rank.ORDER, TaxonomicStatus.ACCEPTED);
      Node n4 = neo.create(Origin.DENORMED_CLASSIFICATION, "Plantae", Rank.KINGDOM, TaxonomicStatus.ACCEPTED);
      n4.createRelationshipTo(n3, RelType.PARENT_OF);
      n3.createRelationshipTo(n2, RelType.PARENT_OF);
      n2.createRelationshipTo(n, RelType.PARENT_OF);
      syn.createRelationshipTo(n, RelType.SYNONYM_OF);
      tx.success();

      List<RankedName> classification = Lists.newArrayList();
      assertTrue(neo.matchesClassification(n4, classification));
      assertFalse(neo.matchesClassification(n3, classification));
      assertFalse(neo.matchesClassification(n2, classification));
      assertFalse(neo.matchesClassification(n, classification));
//      assertFalse(neo.matchesClassification(syn, classification));

      classification.add(new RankedName("Plantae", Rank.KINGDOM));
      assertFalse(neo.matchesClassification(n4, classification));
      assertTrue(neo.matchesClassification(n3, classification));
      assertFalse(neo.matchesClassification(n2, classification));
      assertFalse(neo.matchesClassification(n, classification));

      classification.add(0, new RankedName("Pinales", Rank.ORDER));
      assertFalse(neo.matchesClassification(n4, classification));
      assertFalse(neo.matchesClassification(n3, classification));
      assertTrue(neo.matchesClassification(n2, classification));
      assertFalse(neo.matchesClassification(n, classification));

      classification.add(0, new RankedName("Pinaceae", Rank.SUBFAMILY));
      assertFalse(neo.matchesClassification(n4, classification));
      assertFalse(neo.matchesClassification(n3, classification));
      assertFalse(neo.matchesClassification(n2, classification));
      assertFalse(neo.matchesClassification(n, classification));
    }
  }


}