package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.crawler.NormalizerStats;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Main integration tests for the normalizer testing imports of entire small checklists.
 */
public class NormalizerIT extends NeoTest {

  private static final String INCERTAE_SEDIS = "Incertae sedis";
  private NormalizerConfiguration cfg;

  public NormalizerIT() {
    super(false);
  }

  @Before
  public void initDwcaRepo() throws Exception {
    cfg = new NormalizerConfiguration();
    cfg.neo = super.cfg;

    URL dwcasUrl = getClass().getResource("/dwcas");
    Path p = Paths.get(dwcasUrl.toURI());
    cfg.archiveRepository = p.toFile();
  }

  @Test
  public void testIdList() throws Exception {
    final UUID datasetKey = datasetKey(1);

    Normalizer norm = Normalizer.build(cfg, datasetKey, null);
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);

    assertEquals(20, stats.getCount());
    assertEquals(6, stats.getDepth());
    assertEquals(20, stats.getCountByOrigin(Origin.SOURCE));
    assertEquals(1, stats.getRoots());
    assertEquals(4, stats.getSynonyms());

    initDb(datasetKey);
    try (Transaction tx = beginTx()) {
      NameUsage u1 = getUsageByTaxonId("1006");
      NameUsage u2 = getUsageByName("Leontodon taraxacoides");
      NameUsage u3 = getUsageByKey(u1.getKey());

      assertEquals(u1, u2);
      assertEquals(u1, u3);

      NameUsage syn = getUsageByName("Leontodon leysseri");
      NameUsage acc = getUsageByTaxonId("1006");
      assertEquals(acc.getKey(), syn.getAcceptedKey());
    }
  }

  /**
   * Imports should not create implicit genus or species and use the exact, original taxonomy.
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
   * @param key
   */
  private void printKey(Integer key){
    if (key == null) {
      System.out.println("Key: NULL");
    } else {
      System.out.println("Key: " + key + " = " + getUsageByKey(key).getScientificName());
    }
  }

  /**
   * Tests the index fungorum format using a denormed classification.
   * All records of the genus Zignoëlla have been included in the test resources,
   * as only when all are present the original issue of missing higher taxa shows up.
   * <p/>
   * The genus field is left out in the meta.xml, as it causes confusion when the genus is regarded as a synonyms,
   * but there are species within that genus still being accepted. An oddity of the nomenclatoral index fungorum
   * database.
   * Discovered with this test.
   */
  @Test
  public void testDenormedIndexFungorum() throws Exception {
    NormalizerStats stats = normalize(6);
    assertEquals(1, stats.getRoots());

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

      assertUsage("140283",
                  true,
                  "Polystictus substipitatus (Murrill) Sacc. & Trotter",
                  "Coriolus substipitatus Murrill",
                  "Trametes modesta (Kunze ex Fr.) Ryvarden",
                  Rank.SPECIES,
                  "Polystictus",
                  "Hymenochaetaceae",
                  "Hymenochaetales",
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

      assertUsage("970",
                  false,
                  "Chaetosphaeria Tul. & C. Tul.",
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

  @Test
  @Ignore
  public void testProParteSynonyms() throws Exception {
    NormalizerStats stats = normalize(8);
    try (Transaction tx = beginTx()) {
      assertEquals(1, stats.getRoots());
      assertEquals(17, stats.getCount());

      // genus synonym
      NameUsage nu = getUsageByTaxonId("101");
      assertEquals("Cladendula Döring", nu.getScientificName());
      assertEquals(Rank.GENUS, nu.getRank());
      assertEquals(TaxonomicStatus.SYNONYM, nu.getTaxonomicStatus());
      assertTrue(nu.isSynonym());

      NameUsage acc = getUsageByKey(nu.getParentKey());
      assertEquals("Calendula L.", acc.getScientificName());
      assertEquals(Rank.GENUS, acc.getRank());
      assertEquals(TaxonomicStatus.ACCEPTED, acc.getTaxonomicStatus());
      assertFalse(acc.isSynonym());

      // pro parte synonym
      nu = getUsageByTaxonId("1001");
      assertEquals("Calendula eckerleinii Ohle", nu.getScientificName());
      assertEquals(Rank.SPECIES, nu.getRank());
      assertEquals(TaxonomicStatus.PROPARTE_SYNONYM, nu.getTaxonomicStatus());
      assertTrue(nu.isSynonym());

      List<NameUsage> propartes =
        Lists.newArrayList(); // = usageService.search(null, null, "Calendula eckerleinii Ohle", SearchType.fullname, 110, null, null, null, null, null, null);
      assertEquals(3, propartes.size());
      for (NameUsage u : propartes) {
        assertEquals("Calendula eckerleinii Ohle", u.getScientificName());
        //TODO: what status should this record have? The original given or an interpreted ProParte one?
        //assertEquals(TaxonomicStatus.Proparte_Synonym, u.getTaxonomicStatus());
        assertEquals(Rank.SPECIES, u.getRank());
        assertTrue(u.isSynonym() == true);

        acc = getUsageByKey(u.getParentKey());
        assertTrue(acc.isSynonym() == false);
        assertEquals(TaxonomicStatus.ACCEPTED, acc.getTaxonomicStatus());
        if (acc.getScientificName().equals("Calendula arvensis (Vaill.) L.")) {
          assertEquals(Rank.SPECIES, acc.getRank());
        } else if (acc.getScientificName().equals("Calendula incana Willd. subsp. incana")) {
          assertEquals(Rank.SUBSPECIES, acc.getRank());
        } else if (acc.getScientificName().equals("Calendula incana subsp. maderensis (DC.) Ohle")) {
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
      final NameUsage incertae = getUsageByKey( getUsageByTaxonId("10002").getAcceptedKey() );
      assertEquals(incana.getKey(), incertae.getParentKey());
      for (Integer id : new Integer[] {10003, 10004}) {
        assertEquals("Synonym cycle for taxonID 10002 not cut", incertae.getKey(), getUsageByTaxonId(id.toString()).getAcceptedKey());
      }
    }
  }

  /**
   * Testing CLIMBER dataset from ZooKeys:
   * http://www.gbif.org/dataset/e2bcea8c-dfea-475e-a4ae-af282b4ea1c5
   *
   * Especially the behavior of acceptedNameUsage (canonical form withut authorship)
   * pointing to itself (scientificName WITH authorship) indicating this is NOT a synonym.
   */
  @Test
  public void testVerbatimAccepted() throws Exception {
    final UUID datasetKey = datasetKey(14);

    Normalizer norm = Normalizer.build(cfg, datasetKey, null);
    norm.run();
    NormalizerStats stats = norm.getStats();
    System.out.println(stats);

    assertEquals(16, stats.getCount());
    assertEquals(6, stats.getDepth());
    assertEquals(10, stats.getCountByOrigin(Origin.SOURCE));
    assertEquals(1, stats.getRoots());
    assertEquals(0, stats.getSynonyms());

    initDb(datasetKey);
    try (Transaction tx = beginTx()) {
      NameUsage u1 = getUsageByTaxonId("Aglais io");
      NameUsage u2 = getUsageByName("Aglais io");
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

  public static UUID datasetKey(Integer x) throws NormalizationFailedException {
    return UUID.fromString(String.format("%08d-c6af-11e2-9b88-00145eb45e9a", x));
  }

  private NormalizerStats normalize(Integer dKey) throws NormalizationFailedException {
    UUID datasetKey = datasetKey(dKey);
    Normalizer norm = Normalizer.build(cfg, datasetKey, null);
    norm.run();
    NormalizerStats stats = norm.getStats();

    System.out.println(stats);
    initDb(datasetKey);
    return stats;
  }
}