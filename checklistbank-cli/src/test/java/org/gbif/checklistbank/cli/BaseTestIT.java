package org.gbif.checklistbank.cli;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.RelType;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Simple test for the base test class to verify the get methods work fine.
 */
public class BaseTestIT extends BaseTest {

  private NameUsage buildUsage(String taxonID, String name, Rank rank, TaxonomicStatus status) {
    return buildUsage(taxonID, name, name, rank, status);
  }

  private NameUsage buildUsage(String taxonID, String sciName, String canonName, Rank rank, TaxonomicStatus status) {
    NameUsage u = new NameUsage();
    u.setScientificName(sciName);
    u.setCanonicalName(canonName);
    u.setRank(rank);
    u.setTaxonID(taxonID);
    u.setTaxonomicStatus(status);
    return u;
  }

  @Test
  public void testGetUsage() throws Exception {
    initDb();

    NameUsage f = buildUsage("t1", "Pinaceae", Rank.FAMILY, TaxonomicStatus.ACCEPTED);
    NameUsage sp = buildUsage("t2", "Abies alba", Rank.SPECIES, TaxonomicStatus.ACCEPTED);
    NameUsage syn = buildUsage("t3", "Picea alba", Rank.SPECIES, TaxonomicStatus.SYNONYM);

    try (Transaction tx = beginTx()) {
      Node fn = dao.create(f);
      Node spn = dao.create(sp);
      Node synn = dao.create(syn);

      fn.createRelationshipTo(spn, RelType.PARENT_OF);
      synn.createRelationshipTo(spn, RelType.SYNONYM_OF);
      synn.createRelationshipTo(spn, RelType.BASIONYM_OF);

      tx.success();
    }

    try (Transaction tx = beginTx()) {
      NameUsage f2 = getUsageByName("Pinaceae");
      NameUsage sp2 = getUsageByTaxonId("t2");
      NameUsage syn2 = getUsageByTaxonId("t3");

      assertEquals("Pinaceae", f2.getScientificName());
      assertEquals("Abies alba", sp2.getScientificName());
      assertEquals("Picea alba", syn2.getScientificName());

      assertEquals(syn2, getUsageByKey(sp2.getBasionymKey()));
      assertEquals(sp2, getUsageByKey(syn2.getAcceptedKey()));
      assertEquals(f2, getUsageByKey(sp2.getParentKey()));
    }
  }

  @Test
  public void testIndexPerformance() throws Exception {
    initDb();
    try (Transaction tx = beginTx()) {
      dao.getNeo().schema().indexFor(Labels.TAXON).on(NeoProperties.TAXON_ID).create();
      dao.getNeo().schema().indexFor(Labels.TAXON).on(NeoProperties.SCIENTIFIC_NAME).create();
      dao.getNeo().schema().indexFor(Labels.TAXON).on(NeoProperties.CANONICAL_NAME).create();
      tx.success();
    }

    int x = 100;
    System.out.println("Insert "+x+" nodes");
    try (Transaction tx = beginTx()) {
      while (x > 0) {
        dao.create(species(x));
        x--;
      }
      tx.success();
    }

    try (Transaction tx = beginTx()) {
      assertNotNull(getUsageByName(species(12).getScientificName()));
      assertNotNull(getUsageByTaxonId("t12"));
    }
  }

  private NameUsage species(int x) {
    NameUsage u = new NameUsage();
    final String name = "t"+x;
    u.setTaxonID(name);
    u.setCanonicalName(name);
    u.setScientificName(name+" Miller");
    return u;
  }
}