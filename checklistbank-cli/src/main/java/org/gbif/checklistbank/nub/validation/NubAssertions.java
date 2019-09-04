package org.gbif.checklistbank.nub.validation;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.model.Classification;
import org.gbif.checklistbank.nub.NubDb;
import org.gbif.utils.file.InputStreamUtils;
import org.gbif.utils.file.csv.CSVReader;
import org.gbif.utils.file.csv.CSVReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Production backbone assertions that have to pass before we can replace the backbone with a newer version.
 * This acts on name usage used as input to the postgres importer
 */
public class NubAssertions implements NubValidation {
  private static final Logger LOG = LoggerFactory.getLogger(NubAssertions.class);
  private static final String ASSERTION_FILENAME = "backbone/assertions.tsv";
  private static final String HOMONYM_FILENAME = "backbone/homonym-assertions.csv";
  private static final Joiner SEMICOLON_JOIN = Joiner.on("; ");
  
  private String assertionFile;
  private String homonymFile;
  private final AssertionEngine assertionEngine;

  public NubAssertions(NubDb db) {
    this.assertionEngine = new NeoAssertionEngine(db);
  }
  
  public NubAssertions(NameUsageService pgService) {
    this.assertionEngine = new PgAssertionEngine(pgService);
  }
  
  public void setAssertionFile(File assertionFile) {
    Preconditions.checkArgument(assertionFile.exists(), "Assertions file does not exist: " + assertionFile.getAbsolutePath());
    LOG.info("Use external assertions file: " + assertionFile.getAbsolutePath());
    this.assertionFile = assertionFile.getAbsolutePath();
  }
  
  public void setHomonymFile(File file) {
    Preconditions.checkArgument(file.exists(), "Homonym file does not exist: " + file.getAbsolutePath());
    LOG.info("Use external homonym file: " + file.getAbsolutePath());
    this.homonymFile = file.getAbsolutePath();
  }

  @Override
  /**
   * This requires an open neo4j transaction!
   */
  public boolean validate() {
    LOG.info("Start nub assertions");
  
    // run simple file based assertions based on usage ids
    assertFileNames();
  
    assertFileNameHomonyms();

    return assertionEngine.isValid();
  }

  private void assertFileNames() {
    LOG.info("Run usage assertions");
    try {
      InputStream tsv;
      if (assertionFile == null) {
        tsv = new InputStreamUtils().classpathStream(ASSERTION_FILENAME);
      } else {
        tsv = new FileInputStream(assertionFile);
      }
      CSVReader csv = CSVReaderFactory.buildUtf8TabReader(tsv);
      while(csv.hasNext()) {
        String[] row = csv.next();
        if (row == null || row.length < 11 || row[0].startsWith("#")) {
          continue;
        }
        assertRow(row);
      }

    } catch (IOException e) {
      LOG.warn("Failed to read assertion resource", e);
    }
  }

  private void assertFileNameHomonyms() {
    LOG.info("Run homonym assertions");
    try {
      InputStream tsv;
      if (homonymFile == null) {
        tsv = new InputStreamUtils().classpathStream(HOMONYM_FILENAME);
      } else {
        tsv = new FileInputStream(homonymFile);
      }
      CSVReader csv = CSVReaderFactory.build(tsv, "utf8", ";", null, 0);
      while(csv.hasNext()) {
        String[] row = csv.next();
        if (row == null || row.length < 3 || row[0].startsWith("#")) {
          continue;
        }
        try {
          Rank rank= parseRank(row[0]);
          String name = row[1].trim();
          Integer cnt = parseInt(row[2]);
          if (rank == null) {
            assertionEngine.assertSearchMatch(cnt, name);
          } else {
            assertionEngine.assertSearchMatch(cnt, name, rank);
          }
    
        } catch (RuntimeException e) {
          LOG.error("Failed homonym assertion for {}", SEMICOLON_JOIN.join(row), e);
        }
      }
    
    } catch (IOException e) {
      LOG.warn("Failed to read assertion resource", e);
    }
  }

  private static Integer parseInt(String x) {
    if (!StringUtils.isBlank(x)) {
      return Integer.parseInt(x.trim());
    }
    return null;
  }
  
  private static Rank parseRank(String x) {
    if (!StringUtils.isBlank(x)) {
      return Rank.valueOf(x.trim().toUpperCase());
    }
    return null;
  }
  
  // ID	name	rank	synonym	kingdom	phylum	class	order	family
  private void assertRow(String[] row) {
    try {
      int key = Integer.valueOf(row[0]);
      String name = row[1];
      Rank rank = Rank.valueOf(row[2]);
      String accepted = row[3];
      Kingdom kingdom = Kingdom.valueOf(row[4].toUpperCase());

      LinneanClassification classification = new Classification();
      classification.setKingdom(row[4]);
      classification.setPhylum(row[5]);
      classification.setClazz(row[6]);
      classification.setOrder(row[7]);
      classification.setFamily(row[8]);
      classification.setGenus(row[9]);
      classification.setSpecies(row[10]);

      assertionEngine.assertUsage(key, rank, name, accepted, kingdom);
      assertionEngine.assertClassification(key, classification);

    } catch (RuntimeException e) {
      LOG.error("Failed assertion for {}", SEMICOLON_JOIN.join(row), e);
    }
  }

  /**
   * Patches from Jonathan: https://github.com/OpenTreeOfLife/reference-taxonomy/blob/master/taxonomies.py#L562
   * DEACTIVATED AS THEY ARE OUTDATED AND WILL FAIL !!!
   */
  private void assertOtolIssues() {
    // http://iphylo.blogspot.com/2014/03/gbif-liverwort-taxonomy-broken.html
    assertionEngine.assertSearchMatch(1, "Jungermanniales", Rank.ORDER);
    assertionEngine.assertUsage(7219205, Rank.ORDER, "Jungermanniales", null, Kingdom.PLANTAE);
    assertionEngine.assertClassification(7219205, "Jungermanniopsida", "Bryophyta", "Plantae");

    // Joseph 2013-07-23 https://github.com/OpenTreeOfLife/opentree/issues/62
    // GBIF has 3 copies of Myospalax, but only one accepted
    assertionEngine.assertSearchMatch(3, "Myospalax", Rank.GENUS);
    assertionEngine.assertUsage(7427330, Rank.GENUS, "Myospalax Hermann, 1783", "Spalax", Kingdom.ANIMALIA);
    assertionEngine.assertUsage(8188734, Rank.GENUS, "Myospalax Blyth, 1846", "Ellobius", Kingdom.ANIMALIA);
    // accepted
    assertionEngine.assertUsage(2439119, Rank.GENUS, "Myospalax Laxmann, 1769", null, Kingdom.ANIMALIA);
    assertionEngine.assertClassification(2439119, "Spalacidae", "Rodentia", "Mammalia", "Chordata", "Animalia");

    // Drake-brockmania & Drake-Brockmania should be one
    assertionEngine.assertNotExisting("Drake-brockmania", Rank.GENUS);
    assertionEngine.assertUsage(4120905, Rank.GENUS, "Drake-Brockmania", null, Kingdom.PLANTAE);
    //TODO: find ID
    assertionEngine.assertUsage(-1, Rank.GENUS, "Drakebrockmania", null, Kingdom.PLANTAE);

    // Saxo-Fridericia vs Saxo-fridericia (COL!) vs Saxofridericia
    assertionEngine.assertNotExisting("Saxo-Fridericia", Rank.GENUS);
    assertionEngine.assertNotExisting("Saxofridericia", Rank.GENUS);
    assertionEngine.assertUsage(7276512, Rank.GENUS, "Saxo-fridericia", null, Kingdom.PLANTAE);

    assertionEngine.assertNotExisting("Solms-Laubachia", Rank.GENUS);
    assertionEngine.assertNotExisting("Solmslaubachia", Rank.GENUS);
    assertionEngine.assertUsage(3044438, Rank.GENUS, "Solms-laubachia", null, Kingdom.PLANTAE);

    assertionEngine.assertNotExisting("Cyrto-Hypnum", Rank.GENUS);
    assertionEngine.assertNotExisting("Cyrtohypnum", Rank.GENUS);
    assertionEngine.assertUsage(2673193, Rank.GENUS, "Cyrto-hypnum", null, Kingdom.PLANTAE);
  }

  /**
   * From https://gbif-ecat.googlecode.com/svn/trunk/ecat-checklistbank/src/test/java/org/gbif/manualtests/VerifyNub.java
   * DEACTIVATED AS THEY ARE OUTDATED AND WILL FAIL !!!
   */
  private void oldEcatVerifications() {
    LOG.info("Test kingdoms");
    assertionEngine.assertUsage(0, Rank.KINGDOM, "incertae sedis", null, Kingdom.INCERTAE_SEDIS);
    assertionEngine.assertUsage(1, Rank.KINGDOM, "Animalia", null, Kingdom.ANIMALIA);
    assertionEngine.assertUsage(2, Rank.KINGDOM, "Archaea", null, Kingdom.ARCHAEA);
    assertionEngine.assertUsage(3, Rank.KINGDOM, "Bacteria", null, Kingdom.BACTERIA);
    assertionEngine.assertUsage(4, Rank.KINGDOM, "Chromista", null, Kingdom.CHROMISTA);
    assertionEngine.assertUsage(5, Rank.KINGDOM, "Fungi", null, Kingdom.FUNGI);
    assertionEngine.assertUsage(6, Rank.KINGDOM, "Plantae", null, Kingdom.PLANTAE);
    assertionEngine.assertUsage(7, Rank.KINGDOM, "Protozoa", null, Kingdom.PROTOZOA);
    assertionEngine.assertUsage(8, Rank.KINGDOM, "Viruses", null, Kingdom.VIRUSES);

    LOG.info("Test Puma");
    assertionEngine.assertUsage(2435099, Rank.SPECIES, "Puma concolor (Linnaeus, 1771)", null, Kingdom.ANIMALIA);
    assertionEngine.assertUsage(2435104, Rank.SPECIES, "Felis concolor Linnaeus, 1771", "Puma concolor", Kingdom.ANIMALIA);

    // http://dev.gbif.org/issues/browse/CLB-72
    LOG.info("Test stable ids issue CLB-72");
    // assorted
    assertionEngine.assertUsage(5304574, Rank.SPECIES, "Dracaena cinnabari Balf.", null, Kingdom.PLANTAE);
    assertionEngine.assertUsage(5214860, Rank.SPECIES, "Zeus faber sp. mauritanicus Desbrosses, 1937", "Zeus faber Linnaeus, 1758", Kingdom.ANIMALIA);
    assertionEngine.assertUsage(4404259, Rank.SPECIES, "Hydraena truncata Rey, 1885", null, Kingdom.ANIMALIA);
    assertionEngine.assertUsage(5346048, Rank.SPECIES, "Astragalus angustifolius Lam.", null, Kingdom.PLANTAE);
    assertionEngine.assertUsage(5358748, Rank.SPECIES, "Trifolium repens L.", null, Kingdom.PLANTAE);
    assertionEngine.assertUsage(5219173, Rank.SPECIES, "Canis lupus Linnaeus, 1758", null, Kingdom.ANIMALIA);
    // species with genus homonyms, aka "partial" homonyms
    assertionEngine.assertUsage(5277309, Rank.SPECIES, "Acanthophora aokii Okamura", null, Kingdom.PLANTAE);
    // homonym families
    assertionEngine.assertSearchMatch(2, "Axiidae ", Rank.FAMILY);
    assertionEngine.assertUsage(8024, Rank.FAMILY, "Axiidae Rebel", "Cimeliidae", Kingdom.ANIMALIA);
    assertionEngine.assertUsage(8026, Rank.FAMILY, "Axiidae Huxley", null, Kingdom.ANIMALIA);
    assertionEngine.assertParentsContain(8024, Rank.CLASS, "Malacostraca");
    assertionEngine.assertParentsContain(8026, Rank.CLASS, "Insecta");

    // 2 authorities exist L. Agassiz, 1862   AND   Berlese, 1896
    assertionEngine.assertSearchMatch(2, "Cepheidae ", Rank.FAMILY);
    assertionEngine.assertUsage(8172, Rank.FAMILY, "Cepheidae", null, Kingdom.ANIMALIA);
    assertionEngine.assertParentsContain(8172, Rank.PHYLUM, "Cnidaria");
    assertionEngine.assertParentsContain(8172, Rank.CLASS, "Scyphozoa");
    assertionEngine.assertUsage(7410, Rank.FAMILY, "Cepheidae", null, Kingdom.ANIMALIA);
    assertionEngine.assertParentsContain(7410, Rank.PHYLUM, "Arthropoda");
    assertionEngine.assertParentsContain(7410, Rank.CLASS, "Arachnida");

    LOG.info("Test higher taxa");
    assertionEngine.assertUsage(212, Rank.CLASS, "Aves", null, Kingdom.ANIMALIA);
    assertionEngine.assertUsage(358, Rank.CLASS, "Reptilia", null, Kingdom.ANIMALIA);
    assertionEngine.assertUsage(359, Rank.CLASS, "Mammalia", null, Kingdom.ANIMALIA);
    assertionEngine.assertUsage(3065, Rank.FAMILY, "Asteraceae", null, Kingdom.PLANTAE);
    assertionEngine.assertUsage(6070956, Rank.FAMILY, "Compositae", "Asteraceae", Kingdom.PLANTAE);

    LOG.info("Test Pachycephala");
    assertionEngine.assertUsage(6006971, Rank.GENUS, "Pachycephala Lioy, 1864", null, Kingdom.ANIMALIA);
    assertionEngine.assertClassification(6006971, "Tachinidae", "Diptera", "Insecta", "Arthropoda", "Animalia");
    assertionEngine.assertUsage(5959160, Rank.GENUS, "Pachycephala Vigors, 1825", null, Kingdom.ANIMALIA);
    assertionEngine.assertClassification(5959160, "Corvidae", "Passeriformes", "Aves", "Chordata", "Animalia");
    assertionEngine.assertUsage(6007723, Rank.GENUS, "Pachycephala Klug, 1834", "Platychile Macleay", Kingdom.ANIMALIA);
    assertionEngine.assertClassification(6007723, "Platychile", "Carabidae", "Coleoptera", "Insecta", "Arthropoda", "Animalia");

    LOG.info("Test Oenanthes");
    assertionEngine.assertUsage(2492483, Rank.GENUS, "Oenanthe Vieillot, 1816", null, Kingdom.ANIMALIA);
    assertionEngine.assertUsage(3034893, Rank.GENUS, "Oenanthe Linnaeus, 1753", null, Kingdom.PLANTAE);
    assertionEngine.assertSearchMatch(2, "Oenanthe ", Rank.GENUS);
    assertionEngine.assertSearchMatch(0, "Oenanthe spec", Rank.SPECIES);

    LOG.info("Test aves classification");
    assertionEngine.assertUsage(212, Rank.CLASS, "Aves", null, Kingdom.ANIMALIA);
    assertionEngine.assertClassification(212, "Reptilia", "Chordata", "Animalia");
    assertionEngine.assertUsage(358, Rank.CLASS, "Reptilia", null, Kingdom.ANIMALIA);
    assertionEngine.assertUsage(44, Rank.PHYLUM, "Chordata", null, Kingdom.ANIMALIA);

    LOG.info("Test beetles");
    assertionEngine.assertParentsContain("Syntomus", Rank.GENUS, "Carabidae");
    assertionEngine.assertParentsContain("Thalassophilus", Rank.GENUS, "Carabidae");
    assertionEngine.assertParentsContain("Lionychus", Rank.GENUS, "Carabidae");
    assertionEngine.assertParentsContain("Oxypselaphus", Rank.GENUS, "Carabidae");
    assertionEngine.assertParentsContain("Bembidion", Rank.GENUS, "Carabidae");
    // family unknown, but a beetle
    assertionEngine.assertParentsContain("Lastia", Rank.GENUS, "Coleoptera");

    // http://dev.gbif.org/issues/browse/CLB-83
    LOG.info("Test Fucus");
    assertionEngine.assertUsage(1010512, Rank.GENUS, "Fucus L.", null, Kingdom.CHROMISTA);
    assertionEngine.assertClassification(1010512, "Fucaceae", "Fucales", "Phaeophyceae", "Ochrophyta", "Chromista");

    LOG.info("Test various");
    assertionEngine.assertUsage(2765628, Rank.GENUS, "Astelia Banks & Sol. ex R. Br.", null, Kingdom.PLANTAE);
    assertionEngine.assertSearchMatch(1, "Astelia( |$)", Rank.GENUS);
    assertionEngine.assertUsage(6014709, Rank.SPECIES, "Tulostoma exasperatum Mont.", null, Kingdom.FUNGI);
    // http://dev.gbif.org/issues/browse/CLB-70
    assertionEngine.assertSearchMatch(1, "Tulostoma exasperatum", Rank.SPECIES);
    assertionEngine.assertUsage(2295111, Rank.GENUS, "Chloritis Beck, 1837", null, Kingdom.ANIMALIA);
    assertionEngine.assertClassification(2295111, "Camaenidae", "Stylommatophora", "Gastropoda", "Mollusca", "Animalia");
    // see http://dev.gbif.org/issues/browse/CLB-69
    assertionEngine.assertSearchMatch(1, "Trifolium repens", Rank.SPECIES);
    // Fucus http://dev.gbif.org/issues/browse/CLB-83
    assertionEngine.assertSearchMatch(1, "Fucus( |$)", Rank.GENUS);
    assertionEngine.assertClassification(1010512, "Fucaceae", "Fucales", "Phaeophyceae", "Ochrophyta", "Chromista");
    // bird Zonotrichia albicollis http://dev.gbif.org/issues/browse/CLB-119
    assertionEngine.assertUsage(5231140, Rank.SPECIES, "Zonotrichia albicollis (Gmelin, 1789)", null, Kingdom.ANIMALIA);
    assertionEngine.assertClassification(5231140, "Zonotrichia", "Emberizidae", "Passeriformes", "Aves", "Chordata", "Animalia");

    LOG.info("Test inter rank homonyms");
    // http://dev.gbif.org/issues/browse/CLB-63
    assertionEngine.assertSearchMatch(2, "Lobata");
    assertionEngine.assertSearchMatch(2, "Coccoidea");
    // 2 genera, 1 class
    assertionEngine.assertSearchMatch(3, "Acantharia");
    assertionEngine.assertSearchMatch(1, "Acantharia", Rank.CLASS);
    assertionEngine.assertSearchMatch(2, "Acantharia", Rank.GENUS);
    // the subphylum isnt part of the nub
    assertionEngine.assertSearchMatch(1, "Radiolaria");

    LOG.info("Test duplicate names");
    // http://dev.gbif.org/issues/browse/CLB-126
    assertionEngine.assertSearchMatch(1, "Orbicularia foveolata", Rank.SPECIES);
    assertionEngine.assertUsage(6638224, Rank.SPECIES, "Orbicularia foveolata Britton", "Phyllanthus myrtilloides subsp. erythrinus", Kingdom.PLANTAE);
    assertionEngine.assertSearchMatch(1, "Nymania insignis", Rank.SPECIES);
    assertionEngine.assertUsage(6046058, Rank.SPECIES, "Nymania insignis K.Schum.", "Phyllanthus clamboides", Kingdom.PLANTAE);
    assertionEngine.assertSearchMatch(1, "Phyllanthodendron lingulatum", Rank.SPECIES);
    assertionEngine.assertSearchMatch(1, "Acidoton flueggeoides", Rank.SPECIES);
    assertionEngine.assertSearchMatch(1, "Villanova buxifolia", Rank.SPECIES);
    assertionEngine.assertSearchMatch(1, "Colmeiroa buxifolia", Rank.SPECIES);
    assertionEngine.assertSearchMatch(1, "Maschalanthus obovatus", Rank.SPECIES);
    // homonym genus
    assertionEngine.assertSearchMatch(3, "Wielandia", Rank.GENUS);
    assertionEngine.assertSearchMatch(1, "Wielandia danguyana", Rank.SPECIES);

    LOG.info("Test fungal names");
    assertionEngine.assertUsage(6348821, Rank.VARIETY, "Hendersonia sarmentorum var. catalpae Sandu", null, Kingdom.FUNGI);
    assertionEngine.assertUsage(6332962, Rank.VARIETY, "Ithyphallus aurantiacus var. gracilis E. Fisch.", null, Kingdom.FUNGI);
    assertionEngine.assertUsage(6336702, Rank.VARIETY, "Microsphaera alphitoides var. chenii U. Braun", null, Kingdom.FUNGI);
    assertionEngine.assertUsage(6342638, Rank.VARIETY, "Peltidea canina var. glabra Ach.", null, Kingdom.FUNGI);
    assertionEngine.assertUsage(6774914, Rank.VARIETY, "Placodium boergesenii var. squamosoareolata Vain.", null, Kingdom.FUNGI);
    assertionEngine.assertUsage(6329816, Rank.VARIETY, "Togaria aurea var. aurea (Matt.) W.G. Sm.", null, Kingdom.FUNGI);
    assertionEngine.assertUsage(6340009, Rank.VARIETY, "Nesolechia xenophana var. hazslinszkyana Keissl.", null, Kingdom.FUNGI);
    assertionEngine.assertUsage(6328015, Rank.VARIETY, "Omphalia lignatilis var. albovirens QuÃ©l.", null, Kingdom.FUNGI);
    //ID had changed before, why?
    assertionEngine.assertUsage(7252140, Rank.SPECIES, "Briarea orbicula (Corda) Bonord.", null, Kingdom.FUNGI);
    assertionEngine.assertUsage(6012748, Rank.FORM, "Echinoderma acutesquamosum f. giganteum (PilÃ¡t) Bon", null, Kingdom.FUNGI);

    //instable autonym ID: http://dev.gbif.org/issues/browse/CLB-183
    LOG.info("Autonym ids");
    assertionEngine.assertUsage(7243638, Rank.VARIETY, "Drosophila squamosa var. squamosa", null, Kingdom.FUNGI);
    // animal homonym genus
    assertionEngine.assertUsage(6330670, Rank.VARIETY, "Drosophila caput-medusae var. caput-medusae (Fr.) KÃ¼hner & Romagn.", null, Kingdom.FUNGI);
    assertionEngine.assertUsage(6350412, Rank.VARIETY, "Parasterina grewiae var. grewiae (Cooke) Bat. & Maia", null, Kingdom.FUNGI);

    LOG.info("Test mollusc names");
    assertionEngine.assertUsage(4366023, Rank.VARIETY, "Bolinus brandaris var. adunca Coen, 1933", "Bolinus brandaris (Linnaeus, 1758)", Kingdom.ANIMALIA);
    assertionEngine.assertClassification(4366023, "Bolinus brandaris", "Bolinus", "Muricidae", "Neogastropoda", "Gastropoda", "Mollusca", "Animalia");
    assertionEngine.assertUsage(5726859, Rank.VARIETY, "Murex brandaris var. aculeatus Philippi, 1836", "Bolinus brandaris (Linnaeus, 1758)", Kingdom.ANIMALIA);
    assertionEngine.assertUsage(4371850, Rank.VARIETY, "Ensis arcuatus var. directus", "Ensis directus (Conrad, 1843)", Kingdom.ANIMALIA);

    LOG.info("Test insect names");
    assertionEngine.assertUsage(6097478, Rank.VARIETY, "Atheta elongatula var. balcanica", null, Kingdom.ANIMALIA);
    assertionEngine.assertClassification(6097478, "Atheta elongatula", "Atheta", "Staphylinidae", "Staphylinoidea", "Coleoptera", "Insecta", "Arthropoda", "Animalia");

    LOG.info("Test Asteraceae");
    assertionEngine.assertSearchMatch(1, "Asteraceae");
    assertionEngine.assertUsage(3065, Rank.FAMILY, "Asteraceae Bercht. & J.Presl", null, Kingdom.PLANTAE);

    LOG.info("Nub verified!");
  }

}
