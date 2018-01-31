package org.gbif.checklistbank.cli.normalizer;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Maps;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.*;
import org.gbif.checklistbank.cli.BaseTest;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.helpers.collection.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * Main integration tests for the normalizer testing imports of entire small checklists.
 */
public class NormalizerTest extends BaseTest {
  private static final Logger LOG = LoggerFactory.getLogger(NormalizerTest.class);

  private static final String INCERTAE_SEDIS = "Incertae sedis";

  @Test
  public void testgetLowestExistingRank() throws Exception {
    LinneanClassification cl = new NameUsage();
    assertNull(Normalizer.getLowestExistingRank(cl));

    cl.setOrder("oda");
    assertEquals(Rank.ORDER, Normalizer.getLowestExistingRank(cl));

    cl.setPhylum("fülum");
    assertEquals(Rank.ORDER, Normalizer.getLowestExistingRank(cl));

    cl.setSubgenus("sappjenuss");
    assertEquals(Rank.SUBGENUS, Normalizer.getLowestExistingRank(cl));
  }

  @Test
  public void testSplitByCommonDelimiters() throws Exception {
    assertThat(Normalizer.splitByCommonDelimiters("gx:1234")).containsExactly("gx:1234");
    assertThat(Normalizer.splitByCommonDelimiters("1234|135286|678231612")).containsExactly("1234", "135286", "678231612");
    assertThat(Normalizer.splitByCommonDelimiters("1234  135286 678231612")).containsExactly("1234", "135286", "678231612");
    assertThat(Normalizer.splitByCommonDelimiters("1234; 135286; 678231612")).containsExactly("1234", "135286", "678231612");
    assertThat(Normalizer.splitByCommonDelimiters("1234,135286 | 67.8231612")).containsExactly("1234,135286", "67.8231612");
  }

  @Test
  public void testNeoIndices() throws Exception {
    final UUID datasetKey = datasetKey(1);

    Normalizer norm = Normalizer.create(cfg, datasetKey);
    norm.run();

    openDb(datasetKey);
    compareStats(norm.getStats());

    Set<String> taxonIndices = Sets.newHashSet();
    taxonIndices.add(NeoProperties.TAXON_ID);
    taxonIndices.add(NeoProperties.SCIENTIFIC_NAME);
    taxonIndices.add(NeoProperties.CANONICAL_NAME);
    try (Transaction tx = beginTx()) {
      Schema schema = dao.getNeo().schema();
      for (IndexDefinition idf : schema.getIndexes(Labels.TAXON)) {
        List<String> idxProps = Iterables.asList(idf.getPropertyKeys());
        assertTrue(idxProps.size() == 1);
        assertTrue(taxonIndices.remove(idxProps.get(0)));
      }

      assertNotNull(Iterators.singleOrNull(dao.getNeo().findNodes(Labels.TAXON, NeoProperties.TAXON_ID, "1001")));
      assertNotNull(Iterators.singleOrNull(dao.getNeo().findNodes(Labels.TAXON, NeoProperties.SCIENTIFIC_NAME, "Crepis bakeri Greene")));
      assertNotNull(Iterators.singleOrNull(dao.getNeo().findNodes(Labels.TAXON, NeoProperties.CANONICAL_NAME, "Crepis bakeri")));

      assertNull(Iterators.singleOrNull(dao.getNeo().findNodes(Labels.TAXON, NeoProperties.TAXON_ID, "x1001")));
      assertNull(Iterators.singleOrNull(dao.getNeo().findNodes(Labels.TAXON, NeoProperties.SCIENTIFIC_NAME, "xCrepis bakeri Greene")));
      assertNull(Iterators.singleOrNull(dao.getNeo().findNodes(Labels.TAXON, NeoProperties.CANONICAL_NAME, "xCrepis bakeri")));
    }
  }

  @Test
  public void testIdList() throws Exception {
    final UUID datasetKey = datasetKey(1);

    Normalizer norm = Normalizer.create(cfg, datasetKey);
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);

    assertEquals(20, stats.getCount());
    assertEquals(6, stats.getDepth());
    assertEquals(20, stats.getCountByOrigin(Origin.SOURCE));
    assertEquals(1, stats.getRoots());
    assertEquals(4, stats.getSynonyms());

    openDb(datasetKey);
    compareStats(stats);

    try (Transaction tx = beginTx()) {
      NameUsage u1 = getUsageByTaxonId("1006");
      NameUsage u2 = getUsageByName("Leontodon taraxacoides (Vill.) Mérat");
      NameUsage u3 = getUsageByKey(u1.getKey());

      assertEquals(u1, u2);
      assertEquals(u1, u3);

      NameUsage syn = getUsageByName("Leontodon leysseri");
      NameUsage acc = getUsageByTaxonId("1006");
      assertEquals(acc.getKey(), syn.getAcceptedKey());

      // metrics
      assertMetrics(getMetricsByTaxonId("101"), 2, 2, 0, 0, 0, 0, 0, 0, 0, 2);
      assertMetrics(getMetricsByTaxonId("1"), 1, 15, 1, 0, 0, 0, 1, 4, 0, 7);
    }
  }

  private void assertMetrics(NameUsageMetrics m, int children, int descendants, int synonyms,
                             int p, int c, int o, int f, int g, int sg, int s) {
    System.out.println(m);
    assertEquals(children, m.getNumChildren());
    assertEquals(descendants, m.getNumDescendants());
    assertEquals(synonyms, m.getNumSynonyms());
    assertEquals(p, m.getNumPhylum());
    assertEquals(c, m.getNumClass());
    assertEquals(o, m.getNumOrder());
    assertEquals(f, m.getNumFamily());
    assertEquals(g, m.getNumGenus());
    assertEquals(sg, m.getNumSubgenus());
    assertEquals(s, m.getNumSpecies());
  }

  /**
   * Imports should not insert implicit genus or species and use the exact, original taxonomy.
   */
  @Test
  public void testImplicitSpecies() throws Exception {
    NormalizerStats stats = normalize(2);
    try (Transaction tx = beginTx()) {
      // Agaricaceae
      NameUsage fam = getUsageByTaxonId("5");

      // Tulostoma
      NameUsage tulostoma = getUsageByTaxonId("6");

      // Tulostoma
      NameUsage tulostomaEx = getUsageByTaxonId("100");
      assertEquals(tulostoma.getKey(), tulostomaEx.getParentKey());
      NameUsage tulostomaExEx = getUsageByTaxonId("101");
      assertEquals(tulostoma.getKey(), tulostomaExEx.getParentKey());
      NameUsage tulostomaExRid = getUsageByTaxonId("102");
      assertEquals(tulostoma.getKey(), tulostomaExRid.getParentKey());

      // Tulostomafake
      NameUsage tulostomafake1 = getUsageByTaxonId("301");
      assertEquals(fam.getKey(), tulostomafake1.getParentKey());
      NameUsage tulostomafake2 = getUsageByTaxonId("302");
      assertEquals(fam.getKey(), tulostomafake2.getParentKey());

    }

    System.out.println(stats);
    assertEquals(14, stats.getCount());
    assertEquals(7, stats.getDepth());
    assertEquals(14, stats.getCountByOrigin(Origin.SOURCE));
    assertEquals(1, stats.getRoots());
    assertEquals(0, stats.getSynonyms());
  }

  @Test
  public void testSynonymsWIthMissingAccepted() throws Exception {
    NormalizerStats stats = normalize(3);
    System.out.println(stats);
    assertEquals(9, stats.getCount());
    assertEquals(8, stats.getCountByOrigin(Origin.SOURCE));
    assertEquals(1, stats.getCountByOrigin(Origin.MISSING_ACCEPTED));
    assertEquals(5, stats.getDepth());
    assertEquals(1, stats.getRoots());
    assertEquals(4, stats.getSynonyms());

    try (Transaction tx = beginTx()) {

      //Coleoptera
      NameUsage coleo = getUsageByTaxonId("4");
      assertEquals("Coleoptera", coleo.getScientificName());

      // Pygoleptura nigrella
      NameUsage syn = getUsageByTaxonId("200");
      assertTrue(syn.isSynonym());

      // test for inserted incertae sedis usage
      NameUsage incertae = getUsageByKey(syn.getAcceptedKey());
      assertEquals(INCERTAE_SEDIS, incertae.getScientificName());
      assertFalse(incertae.isSynonym());
      // make sure synonym classification is preserved in incertae sedis
      assertEquals(coleo.getKey(), incertae.getParentKey());

      // make sure synonyms of synonyms are relinked to incertae accepted
      // Leptura nigrella Adams, 1909
      NameUsage synLep = getUsageByTaxonId("100");
      assertTrue(synLep.isSynonym());
      assertEquals(incertae.getKey(), synLep.getAcceptedKey());
      // Leptura nigrella Chagnon, 1917
      synLep = getUsageByTaxonId("101");
      assertTrue(synLep.isSynonym());
      assertEquals(incertae.getKey(), synLep.getAcceptedKey());
    }
  }

  @Test
  public void testColSynonyms() throws Exception {
    NormalizerStats stats = normalize(4);
    try (Transaction tx = beginTx()) {

      // Phoenicus sanguinipennis
      NameUsage acc = getUsageByTaxonId("248320");
      assertEquals("Phoenicus sanguinipennis Lacordaire, 1869", acc.getScientificName());
      assertFalse(acc.isSynonym());

      NameUsage syn = getUsageByTaxonId("282664");
      assertEquals("Phoenicus sanguinipennis Aurivillius, 1912", syn.getScientificName());
      assertTrue(syn.isSynonym());
      assertEquals(acc.getKey(), syn.getAcceptedKey());

      // Anniella pulchra pulchra
      NameUsage species = getUsageByTaxonId("1938166");
      assertFalse(species.isSynonym());

      NameUsage speciesSyn = getUsageByTaxonId("1954175");
      assertTrue(speciesSyn.isSynonym());

      NameUsage ssppulchra = getUsageByTaxonId("1943002");
      assertFalse(ssppulchra.isSynonym());
      assertEquals(ssppulchra.getKey(), speciesSyn.getAcceptedKey());
      assertEquals(species.getKey(), ssppulchra.getParentKey());

      NameUsage sspnigra = getUsageByTaxonId("1943001");
      assertFalse(sspnigra.isSynonym());
    }
  }

  /**
   * Testing the insertion of incertae sedis records for synonyms without an accepted usage.
   * Using real IRMNG Homonym Data:
   * "mol101988","Tubiferidae Cossmann, 1895","Cossmann, 1895",,,"Tubiferidae","Heterostropha","Gastropoda","Mollusca",
   * "Animalia","family","synonym"
   * Also testing the materialisation of verbatim acceptedNameUsage in case they dont exist as records in their own
   * right.
   * Tested using 2 IRMNG homonyms:
   * "hex1088048","Acanthophora Hulst, 1896","Hulst, 1896","Acanthophora",,"Geometridae","Lepidoptera","Insecta",
   * "Arthropoda","Animalia","genus","synonym"
   * "hex1090241","Acanthophora Borgmeier, 1922","Borgmeier, 1922","Acanthophora",,"Phoridae","Diptera","Insecta",
   * "Arthropoda","Animalia","genus","synonym"
   */
  @Test
  public void testIncertaeSedisSynonyms() throws Exception {
    NormalizerStats stats = normalize(5);
    assertEquals(5, stats.getSynonyms());
    assertEquals(2, stats.getCountByOrigin(Origin.MISSING_ACCEPTED));
    assertEquals(3, stats.getCountByOrigin(Origin.VERBATIM_ACCEPTED));

    try (Transaction tx = beginTx()) {

      NameUsage syn = getUsageByTaxonId("por10083");
      assertEquals("Megalithistida", syn.getScientificName());
      assertTrue(syn.isSynonym());

      NameUsage sedis = getUsageByKey(syn.getAcceptedKey());
      assertFalse(sedis.isSynonym());
      assertEquals(INCERTAE_SEDIS, sedis.getScientificName());

      NameUsage parent = getUsageByKey(sedis.getParentKey());
      assertFalse(parent.isSynonym());
      assertEquals("Demospongea", parent.getScientificName());
      assertEquals(Rank.CLASS, parent.getRank());


      // Tubiferidae Cossmann, 1895
      // "mol101988","Tubiferidae Cossmann, 1895","Cossmann, 1895",,,"Tubiferidae","Heterostropha","Gastropoda","Mollusca","Animalia","family","synonym"
      syn = getUsageByTaxonId("mol101988");
      assertEquals("Tubiferidae Cossmann, 1895", syn.getScientificName());
      assertTrue(syn.isSynonym());

      sedis = getUsageByKey(syn.getAcceptedKey());
      assertFalse(sedis.isSynonym());
      assertEquals(INCERTAE_SEDIS, sedis.getScientificName());

      parent = getUsageByKey(sedis.getParentKey());
      assertFalse(parent.isSynonym());
      assertEquals("Heterostropha", parent.getScientificName());
      assertEquals(Rank.ORDER, parent.getRank());

      // Acanthophora Hulst, 1896
      syn = getUsageByTaxonId("hex1088048");
      assertEquals("Acanthophora Hulst, 1896", syn.getScientificName());
      assertTrue(syn.isSynonym());

      NameUsage acc = getUsageByKey(syn.getAcceptedKey());
      assertEquals("Acanthotoca", acc.getScientificName());
      assertFalse(acc.isSynonym());
      assertEquals(syn.getParentKey(), acc.getParentKey());

      parent = getUsageByKey(acc.getParentKey());
      assertFalse(parent.isSynonym());
      assertEquals("Geometridae", parent.getScientificName());
      assertEquals(Rank.FAMILY, parent.getRank());

      // Acanthophora Borgmeier, 1922
      syn = getUsageByTaxonId("hex1090241");
      assertEquals("Acanthophora Borgmeier, 1922", syn.getScientificName());
      assertTrue(syn.isSynonym());

      acc = getUsageByKey(syn.getAcceptedKey());
      assertFalse(acc.isSynonym());
      assertEquals("Acanthophorides", acc.getScientificName());

      parent = getUsageByKey(acc.getParentKey());
      assertFalse(parent.isSynonym());
      assertEquals("Phoridae", parent.getScientificName());
      assertEquals(Rank.FAMILY, parent.getRank());
    }
  }

  /**
   * Debugging method, please leave even if not used
   */
  private void printKey(Integer key) {
    if (key == null) {
      System.out.println("Key: NULL");
    } else {
      System.out.println("Key: " + key + " = " + getUsageByKey(key).getScientificName());
    }
  }

  @Test
  public void testDenormedIndexFungorumSmall() throws Exception {
    NormalizerStats stats = normalize(20);
    assertEquals(1, stats.getRoots());
    assertEquals(9, stats.getCountByOrigin(Origin.SOURCE));
    // all distinct higher taxa but the synonym genera and the explicit genus Chaetosphaeria
    assertEquals(24, stats.getCountByOrigin(Origin.DENORMED_CLASSIFICATION));
    assertEquals(33, stats.getCount());

    try (Transaction tx = beginTx()) {
      // make sure we only have one such genus
      NameUsage u = getUsageByCanonical("Chaetosphaeria");
      assertUsage("970",
          false,
          "Chaetosphaeria Tul. & C.Tul.",
          null,
          null,
          Rank.GENUS,
          "Chaetosphaeriaceae",
          "Chaetosphaeriales",
          "Sordariomycetes",
          "Ascomycota",
          "Fungi");

      // verify all denormed usages
      Map<String, Rank> expected = Maps.newHashMap();
      expected.put("Fungi", Rank.KINGDOM);
      expected.put("Ascomycota", Rank.PHYLUM);
      expected.put("Arthoniomycetes", Rank.CLASS);
      expected.put("Dothideomycetes", Rank.CLASS);
      expected.put("Incertae sedis", Rank.CLASS);
      expected.put("Lecanoromycetes", Rank.CLASS);
      expected.put("Sordariomycetes", Rank.CLASS);
      expected.put("Arthoniales", Rank.ORDER);
      expected.put("Capnodiales", Rank.ORDER);
      expected.put("Chaetosphaeriales", Rank.ORDER);
      expected.put("Lecanorales", Rank.ORDER);
      expected.put("Pleosporales", Rank.ORDER);
      expected.put("Triblidiales", Rank.ORDER);
      expected.put("Arthoniaceae", Rank.FAMILY);
      expected.put("Arthopyreniaceae", Rank.FAMILY);
      expected.put("Chaetosphaeriaceae", Rank.FAMILY);
      expected.put("Mycosphaerellaceae", Rank.FAMILY);
      expected.put("Parmeliaceae", Rank.FAMILY);
      expected.put("Triblidiaceae", Rank.FAMILY);
      expected.put("Arthopyrenia", Rank.GENUS);
      expected.put("Blitridium", Rank.GENUS);
      expected.put("Pseudocercospora", Rank.GENUS);
      expected.put("Septoria", Rank.GENUS);
      expected.put("Sphaerella", Rank.GENUS);

      for (Node n : Iterators.loop(dao.allTaxa())) {
        u = dao.readUsage(n, true);
        if (u.getOrigin() == Origin.DENORMED_CLASSIFICATION) {
          Rank r = expected.remove(u.getScientificName());
          if (r == null) {
            fail("Missing denormed usage " + u.getScientificName());
          } else if (!r.equals(u.getRank())) {
            fail("Wrong rank for denormed usage " + u.getScientificName());
          }
        }
      }
      assertTrue(expected.isEmpty());
    }
  }

  /**
   * Tests the index fungorum format using a denormed classification.
   * All records of the genus Zignoëlla have been included in the test resources,
   * as only when all are present the original issue of missing higher taxa shows up.
   * The genus field is left out in the meta.xml, as it causes confusion when the genus is regarded as a synonyms,
   * but there are species within that genus still being accepted. An oddity of the nomenclatoral index fungorum
   * database.
   * Discovered with this test.
   */
  @Test
  public void testDenormedIndexFungorum() throws Exception {
    NormalizerStats stats = normalize(6);
    assertEquals(1, stats.getRoots());
    assertEquals(226, stats.getCountByOrigin(Origin.SOURCE));
    assertEquals(43, stats.getCountByOrigin(Origin.DENORMED_CLASSIFICATION));

    try (Transaction tx = beginTx()) {
      assertUsage("426221",
          false,
          "Lepiota seminuda var. seminuda",
          null,
          null,
          Rank.VARIETY,
          "Lepiota",
          "Agaricaceae",
          "Agaricales",
          "Agaricomycetes",
          "Basidiomycota",
          "Fungi");

      // test a synonym which should use the classification of the accepted name, not the (wrong) denormed of the synonym
      assertUsage("140283",
          true,
          "Polystictus substipitatus (Murrill) Sacc. & Trotter",
          "Coriolus substipitatus Murrill",
          "Trametes modesta (Kunze ex Fr.) Ryvarden",
          Rank.SPECIES,
          "Trametes",
          "Polyporaceae",
          "Polyporales",
          "Agaricomycetes",
          "Basidiomycota",
          "Fungi");

      assertUsage("233484",
          false,
          "Zignoëlla culmicola Delacr.",
          null,
          null,
          Rank.SPECIES,
          "Zignoëlla",
          "Chaetosphaeriaceae",
          "Chaetosphaeriales",
          "Sordariomycetes",
          "Ascomycota",
          "Fungi");

      // author whitespace has changed as this is an assembled name!
      assertUsage("970",
          false,
          "Chaetosphaeria Tul. & C.Tul.",
          null,
          null,
          Rank.GENUS,
          "Chaetosphaeriaceae",
          "Chaetosphaeriales",
          "Sordariomycetes",
          "Ascomycota",
          "Fungi");
    }
  }

  /**
   * Tests the creation of parent and accepted usages given as verbatim names via acceptedNameUsage or parentNameUsage.
   */
  @Test
  public void testMaterializeVerbatimParents() throws Exception {
    NormalizerStats stats = normalize(7);
    assertEquals(3, stats.getRoots());  // Animalia, Pygoleptura & Pygoleptura synomica
    assertEquals(11, stats.getCount());
    assertEquals(2, stats.getSynonyms());  // Leptura nigrella & Pygoleptura tinktura subsp. synomica
    assertEquals(9, stats.getCountByOrigin(Origin.SOURCE));
    assertEquals(1, stats.getCountByOrigin(Origin.VERBATIM_ACCEPTED));
    assertEquals(1, stats.getCountByOrigin(Origin.VERBATIM_PARENT));
    assertEquals(1, stats.getCountByRank(Rank.KINGDOM));
    assertEquals(1, stats.getCountByRank(Rank.PHYLUM));
    assertEquals(1, stats.getCountByRank(Rank.ORDER));
    assertEquals(4, stats.getCountByRank(Rank.SPECIES));

    try (Transaction tx = beginTx()) {
      NameUsage u = getUsageByTaxonId("100");
      assertFalse(u.isSynonym());
      assertEquals(Rank.SPECIES, u.getRank());
      assertEquals("Leptura tinktura Döring, 2011", u.getScientificName());

      u = getUsageByKey(u.getParentKey());
      Integer coleopteraID = u.getKey();
      assertFalse(u.isSynonym());
      assertEquals(Rank.ORDER, u.getRank());
      assertEquals("Coleoptera", u.getScientificName());

      u = getUsageByKey(u.getParentKey());
      assertFalse(u.isSynonym());
      assertEquals(Rank.CLASS, u.getRank());
      assertEquals("Insecta", u.getScientificName());

      u = getUsageByKey(u.getParentKey());
      assertFalse(u.isSynonym());
      assertEquals(Rank.PHYLUM, u.getRank());
      assertEquals("Arthropoda", u.getScientificName());

      u = getUsageByKey(u.getParentKey());
      assertFalse(u.isSynonym());
      assertEquals(Rank.KINGDOM, u.getRank());
      assertEquals("Animalia", u.getScientificName());

      assertNull(u.getParentKey());

      u = getUsageByTaxonId("101");
      assertTrue(u.isSynonym());
      assertEquals(Rank.SPECIES, u.getRank());
      assertEquals("Leptura nigrella Adams, 1909", u.getScientificName());

      u = getUsageByKey(u.getAcceptedKey());
      assertFalse(u.isSynonym());
      assertEquals(Rank.SPECIES, u.getRank());
      assertEquals("Pygoleptura nigrella", u.getScientificName());
      assertEquals(coleopteraID, u.getParentKey());
      Integer pNigrellaID = u.getKey();

      u = getUsageByTaxonId("102");
      assertEquals(pNigrellaID, u.getKey());

      u = getUsageByTaxonId("103");
      assertFalse(u.isSynonym());
      assertEquals(Rank.SPECIES, u.getRank());
      assertEquals("Pygoleptura tinktura Döring, 2011", u.getScientificName());

      u = getUsageByKey(u.getParentKey());
      assertFalse(u.isSynonym());
      assertNull(u.getRank());
      assertEquals("Pygoleptura", u.getScientificName());

      assertNull(u.getParentKey());

      u = getUsageByTaxonId("104");
      assertEquals(Rank.SUBSPECIES, u.getRank());
      assertEquals("Pygoleptura tinktura subsp. synomica Döring, 2011", u.getScientificName());
      assertTrue(u.isSynonym());

      u = getUsageByKey(u.getAcceptedKey());
      assertFalse(u.isSynonym());
      assertNull(u.getRank());
      assertEquals("Pygoleptura synomica", u.getScientificName());

      assertNull(u.getParentKey());

    }
  }

  /**
   * Pro parte synonyms get exploded into several usages/nodes that each have just one accepted taxon!
   */
  @Test
  public void testProParteSynonyms() throws Exception {
    NormalizerStats stats = normalize(8);
    try (Transaction tx = beginTx()) {
      assertEquals(1, stats.getRoots());
      assertEquals(17, stats.getCount());
      assertEquals(6, stats.getSynonyms());
      assertEquals(17, stats.getCountByOrigin(Origin.SOURCE));
      assertEquals(2, stats.getCountByRank(Rank.GENUS));
      assertEquals(7, stats.getCountByRank(Rank.SPECIES));
      assertEquals(5, stats.getCountByRank(Rank.SUBSPECIES));

      // genus synonym
      NameUsage nu = getUsageByTaxonId("101");
      assertEquals("Cladendula Döring", nu.getScientificName());
      assertEquals(Rank.GENUS, nu.getRank());
      assertEquals(TaxonomicStatus.SYNONYM, nu.getTaxonomicStatus());
      assertTrue(nu.isSynonym());

      NameUsage acc = getUsageByKey(nu.getAcceptedKey());
      assertEquals("Calendula L.", acc.getScientificName());
      assertEquals(Rank.GENUS, acc.getRank());
      assertEquals(TaxonomicStatus.ACCEPTED, acc.getTaxonomicStatus());
      assertFalse(acc.isSynonym());

      // pro parte synonym
      Set<Integer> accIds = Sets.newHashSet();
      List<NameUsage> pps = getUsagesByName("Calendula eckerleinii Ohle");
      assertEquals(1, pps.size());
      for (NameUsage u : pps) {
        assertEquals("Calendula eckerleinii Ohle", u.getScientificName());
        assertEquals(Rank.SPECIES, u.getRank());
        assertEquals(TaxonomicStatus.PROPARTE_SYNONYM, u.getTaxonomicStatus());
        assertTrue(u.isSynonym());
        assertFalse(accIds.contains(u.getAcceptedKey()));
        accIds.add(u.getAcceptedKey());
      }

      for (Integer aid : accIds) {
        acc = getUsageByKey(aid);
        assertFalse(acc.isSynonym());
        assertEquals(TaxonomicStatus.ACCEPTED, acc.getTaxonomicStatus());
        if (acc.getTaxonID().equals("1000")) {
          assertEquals("Calendula arvensis (Vaill.) L.", acc.getScientificName());
          assertEquals(Rank.SPECIES, acc.getRank());
        } else if (acc.getTaxonID().equals("10000")) {
          assertEquals("Calendula incana Willd. subsp. incana", acc.getScientificName());
          assertEquals(Rank.SUBSPECIES, acc.getRank());
        } else if (acc.getTaxonID().equals("10002")) {
          assertEquals("Calendula incana subsp. maderensis (DC.) Ohle", acc.getScientificName());
          assertEquals(Rank.SUBSPECIES, acc.getRank());
        } else {
          fail("Unknown pro parte synonym");
        }
      }
    }
  }

  /**
   * Tests if the same verbatim parent gets reused and only one usage is created for it
   */
  @Test
  public void testVerbatimParent() throws Exception {
    NormalizerStats stats = normalize(11);
    assertEquals(1, stats.getRoots());

    try (Transaction tx = beginTx()) {

      final NameUsage sspalgarbiensis = getUsageByTaxonId("10001");
      assertUsage(sspalgarbiensis, Rank.SUBSPECIES, "Calendula incana subsp. algarbiensis (Boiss.) Ohle", false);

      final NameUsage algarbiensis = getUsageByKey(sspalgarbiensis.getBasionymKey());
      assertUsage(algarbiensis, Rank.SPECIES, "Calendula algarbiensis Boss.", true);

      final NameUsage eckerleinii = getUsageByKey(algarbiensis.getAcceptedKey());
      assertUsage(eckerleinii, Rank.SPECIES, "Calendula eckerleinii Ohle", false);

      final NameUsage incana = getUsageByKey(sspalgarbiensis.getParentKey());
      assertUsage(incana, Rank.SPECIES, "Calendula incana Willd.", false);

      final NameUsage callendula = getUsageByKey(incana.getParentKey());
      assertUsage(callendula, Rank.GENUS, "Calendula L.", false);
      assertEquals(callendula.getKey(), eckerleinii.getParentKey());

      final NameUsage compositae = getUsageByKey(callendula.getParentKey());
      assertUsage(compositae, Rank.FAMILY, "Compositae Giseke", false);

      final NameUsage asteraceae = getUsageByTaxonId("11");
      assertUsage(asteraceae, Rank.FAMILY, "Asteraceae", true);
      assertEquals(compositae.getKey(), asteraceae.getAcceptedKey());

      final NameUsage plant = getUsageByKey(compositae.getParentKey());
      assertUsage(plant, Rank.KINGDOM, "Plantae", false);
    }
  }

  @Test
  public void testDenormedClassification() throws Exception {
    NormalizerStats stats = normalize(12);
    try (Transaction tx = beginTx()) {
      assertUsage("1",
          false,
          "Lepiota seminuda",
          null,
          null,
          Rank.SPECIES,
          "Lepiota",
          "Agaricaceae",
          "Agaricales",
          "Agaricomycetes",
          "Basidiomycota");

      assertUsage("2",
          false,
          "Lepiota seminuda",
          null,
          null,
          Rank.SPECIES,
          "Lepiota",
          "Agaricaceae",
          "Agaricales",
          "Agaricomycetes",
          "Basidiomycota");

      assertUsage("3",
          false,
          "Lepiota seminuda",
          null,
          null,
          Rank.SPECIES,
          "Agaricales",
          "Agaricomycetes",
          "Basidiomycota");

      assertUsage("4",
          false,
          "Lepiota seminuda",
          null,
          null,
          Rank.SPECIES,
          "Lepiota",
          "Agaricaceae",
          "Agaricales",
          "Agaricomycetes",
          "Basidiomycota",
          "Fungi");

      // verify identities of higher taxa
      assertQuantity(2, "Lepiota");
      assertQuantity(2, "Agaricaceae");
      assertQuantity(2, "Agaricales");
      assertQuantity(2, "Agaricomycetes");
    }
  }

  @Test
  public void testMixedDenormedClassification() throws Exception {
    NormalizerStats stats = normalize(13);
    assertEquals(1, stats.getRoots());
    assertEquals(4, stats.getCountByOrigin(Origin.DENORMED_CLASSIFICATION));
    assertEquals(8, stats.getCount());
    try (Transaction tx = beginTx()) {
      assertUsage("1",
          false,
          "Agaricaceae",
          null,
          null,
          Rank.FAMILY,
          "Agaricales",
          "Agaricomycetes",
          "Basidiomycota",
          "Fungi");

      assertUsage("2",
          false,
          "Lepiota",
          null,
          null,
          Rank.GENUS,
          "Agaricaceae",
          "Agaricales",
          "Agaricomycetes",
          "Basidiomycota",
          "Fungi");

      assertUsage("3",
          false,
          "Lepiota seminuda",
          null,
          null,
          Rank.SPECIES,
          "Lepiota",
          "Agaricaceae",
          "Agaricales",
          "Agaricomycetes",
          "Basidiomycota",
          "Fungi");

      // verify identities of higher taxa
      assertQuantity(1, "Lepiota");
      assertQuantity(1, "Agaricaceae");
      assertQuantity(1, "Agaricales");
      assertQuantity(1, "Agaricomycetes");
    }
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2992
   */
  @Test
  public void testMixedDenormedClassification2() throws Exception {
    NormalizerStats stats = normalize(22);
    assertEquals(1, stats.getRoots());
    assertEquals(4, stats.getCountByOrigin(Origin.DENORMED_CLASSIFICATION));
    assertEquals(7, stats.getCount());
    try (Transaction tx = beginTx()) {
      assertUsage("3",
          true,
          "Kamasutra nuda",
          null,
          "Lepiota nuda",
          Rank.SPECIES,
          "Lepiota",
          "Agaricomycetes",
          "Basidiomycota",
          "Fungi");

      // verify identities of higher taxa
      assertQuantity(0, "Kamasutra");
      assertQuantity(1, "Lepiota");
    }
  }

  @Test
  public void testDenormedClassificationBDJ() throws Exception {
    NormalizerStats stats = normalize(17);
    assertEquals(1, stats.getRoots());
    assertEquals(555, stats.getCountByOrigin(Origin.SOURCE));
    assertEquals(232, stats.getCountByOrigin(Origin.DENORMED_CLASSIFICATION));
  }

  private void assertUsage(NameUsage u, Rank rank, String sciName, boolean synonym) {
    assertEquals(synonym, u.isSynonym());
    assertEquals(rank, u.getRank());
    assertEquals(sciName, u.getScientificName());
  }

  private void assertQuantity(Integer expected, String canonical) {
    assertEquals(expected, (Integer) getNodesByName(canonical).size());
  }

  /**
   * Tests the relinking of synonyms that point to other synonyms.
   */
  @Test
  public void testSynonymsOfSynonyms() throws Exception {
    NormalizerStats stats = normalize(9);
    assertEquals(1, stats.getCycles().size());
    assertEquals(1, stats.getRoots());

    try (Transaction tx = beginTx()) {

      final NameUsage incana = getUsageByTaxonId("1000");
      assertFalse(incana.isSynonym());
      assertEquals(Rank.SPECIES, incana.getRank());
      assertEquals("Calendula incana Willd.", incana.getScientificName());

      // synonym chain resolved
      assertEquals(incana.getKey(), getUsageByTaxonId("1001").getAcceptedKey());
      assertEquals(incana.getKey(), getUsageByTaxonId("1002").getAcceptedKey());
      assertEquals(incana.getKey(), getUsageByTaxonId("1003").getAcceptedKey());
      assertEquals(incana.getKey(), getUsageByTaxonId("1004").getAcceptedKey());

      NameUsage u = getUsageByTaxonId("10000");
      assertNull(u.getAcceptedKey());
      assertNull(u.getBasionymKey());
      assertEquals(incana.getKey(), u.getParentKey());

      //the synonym cycle should be cut and all relinked to a new incertae sedis taxon
      final NameUsage incertae = getUsageByKey(getUsageByTaxonId("10002").getAcceptedKey());
      assertEquals(incana.getKey(), incertae.getParentKey());
      for (Integer id : new Integer[]{10003, 10004}) {
        assertEquals("Synonym cycle for taxonID 10002 not cut", incertae.getKey(), getUsageByTaxonId(id.toString()).getAcceptedKey());
      }
    }
  }

  /**
   * Testing CLIMBER dataset from ZooKeys:
   * http://www.gbif.org/dataset/e2bcea8c-dfea-475e-a4ae-af282b4ea1c5
   * Especially the behavior of acceptedNameUsage (canonical form without authorship)
   * pointing to itself (scientificName WITH authorship) indicating this is NOT a synonym.
   */
  @Test
  public void testVerbatimAccepted() throws Exception {
    final UUID datasetKey = datasetKey(14);

    Normalizer norm = Normalizer.create(cfg, datasetKey);
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);

    assertEquals(16, stats.getCount());
    assertEquals(1, stats.getRoots());
    assertEquals(6, stats.getDepth());
    assertEquals(0, stats.getSynonyms());
    assertEquals(10, stats.getCountByOrigin(Origin.SOURCE));
    assertEquals(6, stats.getCountByOrigin(Origin.DENORMED_CLASSIFICATION));
    assertEquals(1, stats.getCountByRank(Rank.KINGDOM));
    assertEquals(1, stats.getCountByRank(Rank.PHYLUM));
    assertEquals(1, stats.getCountByRank(Rank.CLASS));
    assertEquals(1, stats.getCountByRank(Rank.ORDER));
    assertEquals(2, stats.getCountByRank(Rank.FAMILY));
    assertEquals(0, stats.getCountByRank(Rank.GENUS));
    assertEquals(10, stats.getCountByRank(Rank.SPECIES));

    openDb(datasetKey);
    compareStats(stats);

    try (Transaction tx = beginTx()) {
      NameUsage u1 = getUsageByTaxonId("Aglais io");
      NameUsage u2 = getUsageByName("Aglais io (Linnaeus, 1758)");
      assertEquals(u1, u2);

      assertNull(u1.getAcceptedKey());
      assertNull(u1.getAccepted());
      assertFalse(u1.isSynonym());
      assertFalse(u1.getTaxonomicStatus().isSynonym());
      assertEquals(Origin.SOURCE, u1.getOrigin());
    }

    try (Transaction tx = beginTx()) {
      int sourceUsages = 0;
      for (NameUsage u : getAllUsages()) {
        assertNull(u.getAcceptedKey());
        assertNull(u.getAccepted());
        assertNull(u.getBasionymKey());
        assertNull(u.getBasionym());
        assertFalse(u.isSynonym());
        assertFalse(u.getTaxonomicStatus().isSynonym());
        if (u.getTaxonID() != null) {
          assertEquals(Origin.SOURCE, u.getOrigin());
          sourceUsages++;
        } else {
          assertEquals(Origin.DENORMED_CLASSIFICATION, u.getOrigin());
        }
      }
      assertEquals(10, sourceUsages);
    }
  }


  /**
   * Tests the Achillea genus form a VASCAN download
   * with vernacular name, species profile, distribution, description, reference, multimedia and identifier extension.
   */
  @Test
  public void testExtensions() throws Exception {
    final UUID datasetKey = datasetKey(15);

    Normalizer norm = Normalizer.create(cfg, datasetKey);
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);

    assertEquals(57, stats.getCount());
    assertEquals(7, stats.getDepth());
    assertEquals(53, stats.getCountByOrigin(Origin.SOURCE));
    assertEquals(1, stats.getRoots());
    assertEquals(48, stats.getSynonyms());

    openDb(datasetKey);
    compareStats(stats);

    try (Transaction tx = beginTx()) {
      // Achillea
      NameUsage a = getUsageByTaxonId("770");
      // Achillea millefolium
      NameUsage am = getUsageByTaxonId("2768");
      assertEquals(a.getKey(), am.getParentKey());

      //media
      UsageExtensions ea = dao.readExtensions(a.getKey().longValue());
      UsageExtensions eam = dao.readExtensions(am.getKey().longValue());

      assertThat(ea.media).hasSize(0);
      assertThat(eam.media).hasSize(2);
      assertThat(eam.media).extracting("creator").containsOnly("Gary A. Monroe", "J.S. Peterson");
      assertThat(eam.media).extracting("title").containsOnly("Achillea millefolium L. - common yarrow");
      assertThat(eam.media).extracting("identifier").containsOnly(
          URI.create("http://plants.usda.gov/gallery/large/acmi2_002_lvp.jpg"),
          URI.create("http://plants.usda.gov/gallery/pubs/acmi2_006_php.jpg"));
      assertThat(eam.media).extracting("references").containsOnly(
          URI.create("http://plants.usda.gov/java/largeImage?imageID=acmi2_002_avp.jpg"),
          URI.create("http://plants.usda.gov/java/largeImage?imageID=acmi2_006_ahp.tif"));
      assertThat(eam.media).extracting("type").containsOnly(MediaType.StillImage);
      assertThat(eam.media).extracting("format").containsOnly("image/jpg", "image/jpeg");

      //vernaculars
      assertThat(ea.vernacularNames).hasSize(0);
      assertThat(eam.vernacularNames).hasSize(6);
      assertThat(eam.vernacularNames).extracting("language").containsOnly(Language.ENGLISH, Language.FRENCH);
      assertThat(eam.vernacularNames).extracting("country").containsOnly(Country.CANADA);
      assertThat(eam.vernacularNames).extracting("vernacularName").containsOnly(
          "achillée millefeuille", "herbe à dindes", "herbe à dindons", "common yarrow", "yarrow", "milfoil");
    }
  }


  /**
   * http://dev.gbif.org/issues/browse/POR-2755
   */
  @Test
  public void testFloraBrazilIncertaeSedis() throws Exception {
    final UUID datasetKey = datasetKey(18);
    cfg.neo.batchSize = 5;
    Normalizer norm = Normalizer.create(cfg, datasetKey);
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);

    openDb(datasetKey);
    compareStats(stats);

    try (Transaction tx = beginTx()) {

      assertEquals(76, stats.getCount());
      assertEquals(1, stats.getRoots());
      assertEquals(4, stats.getDepth());
      assertEquals(25, stats.getSynonyms());
      assertEquals(50, stats.getCountByOrigin(Origin.SOURCE));
      assertEquals(25, stats.getCountByOrigin(Origin.MISSING_ACCEPTED));
      assertEquals(1, stats.getCountByOrigin(Origin.VERBATIM_PARENT));
      assertEquals(1, stats.getCountByRank(Rank.GENUS));
      assertEquals(62, stats.getCountByRank(Rank.SPECIES));
      assertEquals(3, stats.getCountByRank(Rank.SUBSPECIES));
      assertEquals(9, stats.getCountByRank(Rank.VARIETY));

      // Ceramium rubrum C.Agardh
      NameUsage cr = getUsageByTaxonId("99937");
      assertNotNull(cr);
    }

  }

  /**
   * Tests the simple images media extension
   */
  @Test
  @Ignore
  public void testSimpleImages() throws Exception {

  }

  /**
   * Tests the Audubon media extension
   */
  @Test
  @Ignore
  public void testAudubon() throws Exception {

  }

  /**
   * Tests the EOL media extension
   */
  @Test
  @Ignore
  public void testEolMedia() throws Exception {

  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2819
   */
  @Test
  public void testWormsSubgenus() throws Exception {
    final UUID datasetKey = datasetKey(21);

    Normalizer norm = Normalizer.create(cfg, datasetKey);
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);

    openDb(datasetKey);
    compareStats(stats);
    assertEquals(40, stats.getCountByOrigin(Origin.SOURCE));
    assertEquals(0, stats.getCountByOrigin(Origin.DENORMED_CLASSIFICATION));
    assertEquals(0, stats.getCountByOrigin(Origin.VERBATIM_ACCEPTED));
    assertEquals(0, stats.getCountByOrigin(Origin.MISSING_ACCEPTED));
    assertEquals(1, stats.getRoots());
    assertEquals(17, stats.getSynonyms());
    assertEquals(40, stats.getCount());

    try (Transaction tx = beginTx()) {
      NameUsage subgen = getUsageByTaxonId("171415");
      assertEquals(Rank.SUBGENUS, subgen.getRank());
      assertEquals("Corynonema", subgen.getCanonicalName());
      assertEquals("Hyalonema subgen. Corynonema Ijima, 1927", subgen.getScientificName());

      NameUsage u = getUsageByTaxonId("171426");
      assertEquals(TaxonomicStatus.ACCEPTED, u.getTaxonomicStatus());
      assertEquals(Rank.SPECIES, u.getRank());
      assertEquals("Hyalonema rotundum", u.getCanonicalName());
      assertEquals("Hyalonema rotundum Ijima, 1927", u.getScientificName());
    }
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-3193
   */
  @Test
  public void testWormsDuplicates() throws Exception {
    final UUID datasetKey = datasetKey(23);

    Normalizer norm = Normalizer.create(cfg, datasetKey);
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);

    openDb(datasetKey);
    compareStats(stats);

    try (Transaction tx = beginTx()) {
      print(Rank.CLASS);

      showOrigin(Origin.DENORMED_CLASSIFICATION);

      NameUsage k = getUsageByName("Animalia");
      assertEquals(Rank.KINGDOM, k.getRank());
      assertEquals("Animalia", k.getCanonicalName());
      assertEquals("Animalia", k.getScientificName());
    }

    assertEquals(1, stats.getRoots());
    assertEquals(9, stats.getCountByRank(Rank.KINGDOM));
    assertEquals(5, stats.getCountByRank(Rank.PHYLUM));
    assertEquals(99, stats.getCountByOrigin(Origin.SOURCE));
    assertEquals(12, stats.getCountByOrigin(Origin.DENORMED_CLASSIFICATION));
    assertEquals(0, stats.getCountByOrigin(Origin.VERBATIM_ACCEPTED));
    assertEquals(11, stats.getCountByOrigin(Origin.MISSING_ACCEPTED));
    assertEquals(122, stats.getCount());
  }

  public static UUID datasetKey(Integer x) throws NormalizationFailedException {
    return UUID.fromString(String.format("%08d-c6af-11e2-9b88-00145eb45e9a", x));
  }

  private NormalizerStats normalize(Integer dKey) throws NormalizationFailedException {
    UUID datasetKey = datasetKey(dKey);
    Normalizer norm = Normalizer.create(cfg, datasetKey);
    norm.run();
    NormalizerStats stats = norm.getStats();

    openDb(datasetKey);
    compareStats(stats);
    verifyParsedNames();

    return stats;
  }

  private void verifyParsedNames() {
    try (Transaction tx = beginTx()) {
      for (Node n : dao.getNeo().getAllNodes()) {
        if (!n.hasLabel(Labels.TAXON)) {
          continue;
        }
        ParsedName pn = dao.readName(n.getId());
        assertNotNull(pn);
        assertNotNull(pn.getScientificName());
      }
    }
  }
}