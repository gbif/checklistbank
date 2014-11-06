package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.TaxonProperties;

import java.util.UUID;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Simple test for the base test class to verify the get methods work fine.
 */
public class NeoTestIT extends NeoTest {

  private NormalizerConfiguration cfg;
  private NeoMapper mapper = NeoMapper.instance();

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
    UUID dKey = UUID.randomUUID();
    initDb(dKey);

    NameUsage f = buildUsage("t1", "Pinaceae", Rank.FAMILY, TaxonomicStatus.ACCEPTED);
    NameUsage sp = buildUsage("t2", "Abies alba", Rank.SPECIES, TaxonomicStatus.ACCEPTED);
    NameUsage syn = buildUsage("t3", "Picea alba", Rank.SPECIES, TaxonomicStatus.SYNONYM);

    try (Transaction tx = beginTx()) {
      Node fn = db.createNode(Labels.TAXON);
      mapper.store(fn, f, false);

      Node spn = db.createNode(Labels.TAXON);
      mapper.store(spn, sp, false);

      Node synn = db.createNode(Labels.TAXON);
      mapper.store(synn, syn, false);

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
    final UUID dKey = UUID.randomUUID();
    initDb(dKey);
    try (Transaction tx = beginTx()) {
      db.schema().indexFor(Labels.TAXON).on(TaxonProperties.TAXON_ID).create();
      db.schema().indexFor(Labels.TAXON).on(TaxonProperties.SCIENTIFIC_NAME).create();
      db.schema().indexFor(Labels.TAXON).on(TaxonProperties.CANONICAL_NAME).create();
      tx.success();
    }

    int x = 100;
    System.out.println("Insert "+x+" nodes");
    try (Transaction tx = beginTx()) {
      while (x > 0) {
        Node n = db.createNode(Labels.TAXON);
        n.setProperty(TaxonProperties.TAXON_ID, "t"+x);
        n.setProperty(TaxonProperties.SCIENTIFIC_NAME, species(x)+" Miller");
        n.setProperty(TaxonProperties.CANONICAL_NAME, species(x));
        x--;
      }
      tx.success();
    }

    try (Transaction tx = beginTx()) {
      assertNotNull(getUsageByName(species(12)));
      assertNotNull(getUsageByTaxonId("t12"));
    }
  }

  private String species(int x) {
    return "species "+x;
  }
}