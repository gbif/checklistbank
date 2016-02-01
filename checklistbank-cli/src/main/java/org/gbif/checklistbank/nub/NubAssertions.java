package org.gbif.checklistbank.nub;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.nub.model.NubUsage;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Production backbone assertions that have to pass before we can replace the backbone with a newer version.
 * This acts on name usage used as input to the postgres importer
 */
public class NubAssertions implements TreeValidation {
  private static final Logger LOG = LoggerFactory.getLogger(NubAssertions.class);

  private final NubDb db;
  private final IntIntMap usage2NubKey = new IntIntHashMap();
  private boolean valid = true;

  public NubAssertions(NubDb db) {
    this.db = db;
  }

  @Override
  /**
   * This requires an open neo4j transaction!
   */
  public boolean validate() {
    // populate the reverse key map
    LOG.info("Start nub assertions. Populate the reverse key map first");
    for (Map.Entry<Long, NubUsage> nub : db.dao.nubUsages()) {
      int nubKey = (int) (long) nub.getKey();
      int usageKey = nub.getValue().usageKey;
      Preconditions.checkArgument(!usage2NubKey.containsKey(usageKey), "usageKey " + usageKey + " not unique");
      usage2NubKey.put(usageKey, nubKey);
    }

    // TODO: num accepted in expected range

    // TODO: num accepted per kingdom in expected range

    // TODO: num accepted in Asteraceae, Mammalia, Aves ???

    // TODO: num accepted families, genera

    // http://iphylo.blogspot.com/2014/03/gbif-liverwort-taxonomy-broken.html
    assertSearchMatch(1, "Jungermanniales", Rank.ORDER);
    assertUsage(7219205, Rank.ORDER, "Jungermanniales", true, Kingdom.PLANTAE);
    assertClassification(7219205, "Jungermanniopsida", "Bryophyta", "Plantae");

    oldEcatVerifications();

    return valid;
  }

  /**
   * From https://gbif-ecat.googlecode.com/svn/trunk/ecat-checklistbank/src/test/java/org/gbif/manualtests/VerifyNub.java
   */
  private void oldEcatVerifications() {
    LOG.info("Test kingdoms");
    assertUsage(0, Rank.KINGDOM, "incertae sedis", true, Kingdom.INCERTAE_SEDIS);
    assertUsage(1, Rank.KINGDOM, "Animalia", true, Kingdom.ANIMALIA);
    assertUsage(2, Rank.KINGDOM, "Archaea", true, Kingdom.ARCHAEA);
    assertUsage(3, Rank.KINGDOM, "Bacteria", true, Kingdom.BACTERIA);
    assertUsage(4, Rank.KINGDOM, "Chromista", true, Kingdom.CHROMISTA);
    assertUsage(5, Rank.KINGDOM, "Fungi", true, Kingdom.FUNGI);
    assertUsage(6, Rank.KINGDOM, "Plantae", true, Kingdom.PLANTAE);
    assertUsage(7, Rank.KINGDOM, "Protozoa", true, Kingdom.PROTOZOA);
    assertUsage(8, Rank.KINGDOM, "Viruses", true, Kingdom.VIRUSES);

    LOG.info("Test Puma");
    assertUsage(2435099, Rank.SPECIES, "Puma concolor (Linnaeus, 1771)", true, Kingdom.ANIMALIA);
    assertUsage(2435104, Rank.SPECIES, "Felis concolor Linnaeus, 1771", false, Kingdom.ANIMALIA);

    // http://dev.gbif.org/issues/browse/CLB-72
    LOG.info("Test stable ids issue CLB-72");
    // assorted
    assertUsage(5304574, Rank.SPECIES, "Dracaena cinnabari Balf. f.", true, Kingdom.PLANTAE);
    assertUsage(5214860, Rank.SPECIES, "Zeus faber Linnaeus, 1758", true, Kingdom.ANIMALIA);
    assertUsage(4404259, Rank.SPECIES, "Hydraena truncata Rey, 1885", true, Kingdom.ANIMALIA);
    assertUsage(5346048, Rank.SPECIES, "Astragalus angustifolius Lam.", true, Kingdom.PLANTAE);
    assertUsage(5358748, Rank.SPECIES, "Trifolium repens L.", true, Kingdom.PLANTAE);
    assertUsage(5219173, Rank.SPECIES, "Canis lupus Linnaeus, 1758", true, Kingdom.ANIMALIA);
    // species with genus homonyms, aka "partial" homonyms
    assertUsage(5277309, Rank.SPECIES, "Acanthophora aokii Okamura", true, Kingdom.PLANTAE);
    // homonym families
    assertSearchMatch(2, "Axiidae ", Rank.FAMILY);
    assertUsage(8024, Rank.FAMILY, "Axiidae", true, Kingdom.ANIMALIA);
    assertUsage(8026, Rank.FAMILY, "Axiidae", true, Kingdom.ANIMALIA);
    assertParentsContain(8024, Rank.CLASS, "Malacostraca");
    assertParentsContain(8026, Rank.CLASS, "Insecta");

    // 2 authorities exist L. Agassiz, 1862   AND   Berlese, 1896
    assertSearchMatch(2, "Cepheidae ", Rank.FAMILY);
    assertUsage(8172, Rank.FAMILY, "Cepheidae", true, Kingdom.ANIMALIA);
    assertParentsContain(8172, Rank.PHYLUM, "Cnidaria");
    assertParentsContain(8172, Rank.CLASS, "Scyphozoa");
    assertUsage(7410, Rank.FAMILY, "Cepheidae", true, Kingdom.ANIMALIA);
    assertParentsContain(7410, Rank.PHYLUM, "Arthropoda");
    assertParentsContain(7410, Rank.CLASS, "Arachnida");

    LOG.info("Test higher taxa");
    assertUsage(212, Rank.CLASS, "Aves", true, Kingdom.ANIMALIA);
    assertUsage(358, Rank.CLASS, "Reptilia", true, Kingdom.ANIMALIA);
    assertUsage(359, Rank.CLASS, "Mammalia", true, Kingdom.ANIMALIA);
    assertUsage(3065, Rank.FAMILY, "Asteraceae", true, Kingdom.PLANTAE);
    assertUsage(6070956, Rank.FAMILY, "Compositae", false, Kingdom.PLANTAE);

    LOG.info("Test Pachycephala");
    assertUsage(6006971, Rank.GENUS, "Pachycephala Lioy, 1864", true, Kingdom.ANIMALIA);
    assertClassification(6006971, "Tachinidae", "Diptera", "Insecta", "Arthropoda", "Animalia");
    assertUsage(5959160, Rank.GENUS, "Pachycephala Vigors, 1825", true, Kingdom.ANIMALIA);
    assertClassification(5959160, "Corvidae", "Passeriformes", "Aves", "Chordata", "Animalia");
    assertUsage(6007723, Rank.GENUS, "Pachycephala Klug, 1834", false, Kingdom.ANIMALIA);
    assertClassification(6007723, "Platychile", "Carabidae", "Coleoptera", "Insecta", "Arthropoda", "Animalia");

    LOG.info("Test Oenanthes");
    assertUsage(2492483, Rank.GENUS, "Oenanthe Vieillot, 1816", true, Kingdom.ANIMALIA);
    assertUsage(3034893, Rank.GENUS, "Oenanthe Linnaeus, 1753", true, Kingdom.PLANTAE);
    assertSearchMatch(2, "Oenanthe ", Rank.GENUS);
    assertSearchMatch(0, "Oenanthe spec", Rank.SPECIES);

    LOG.info("Test aves classification");
    assertUsage(212, Rank.CLASS, "Aves", true, Kingdom.ANIMALIA);
    assertClassification(212, "Reptilia", "Chordata", "Animalia");
    assertUsage(358, Rank.CLASS, "Reptilia", true, Kingdom.ANIMALIA);
    assertUsage(44, Rank.PHYLUM, "Chordata", true, Kingdom.ANIMALIA);

    LOG.info("Test beetles");
    assertParentsContain("Syntomus", Rank.GENUS, "Carabidae");
    assertParentsContain("Thalassophilus", Rank.GENUS, "Carabidae");
    assertParentsContain("Lionychus", Rank.GENUS, "Carabidae");
    assertParentsContain("Oxypselaphus", Rank.GENUS, "Carabidae");
    assertParentsContain("Bembidion", Rank.GENUS, "Carabidae");
    // family unknown, but a beetle
    assertParentsContain("Lastia", Rank.GENUS, "Coleoptera");

    // http://dev.gbif.org/issues/browse/CLB-83
    LOG.info("Test Fucus");
    assertUsage(1010512, Rank.GENUS, "Fucus L.", true, Kingdom.CHROMISTA);
    assertClassification(1010512, "Fucaceae", "Fucales", "Phaeophyceae", "Ochrophyta", "Chromista");

    LOG.info("Test various");
    assertUsage(2765628, Rank.GENUS, "Astelia Banks & Sol. ex R. Br.", true, Kingdom.PLANTAE);
    assertSearchMatch(1, "Astelia( |$)", Rank.GENUS);
    assertUsage(6014709, Rank.SPECIES, "Tulostoma exasperatum Mont.", true, Kingdom.FUNGI);
    // http://dev.gbif.org/issues/browse/CLB-70
    assertSearchMatch(1, "Tulostoma exasperatum", Rank.SPECIES);
    assertUsage(2295111, Rank.GENUS, "Chloritis Beck, 1837", true, Kingdom.ANIMALIA);
    assertClassification(2295111, "Camaenidae", "Stylommatophora", "Gastropoda", "Mollusca", "Animalia");
    // see http://dev.gbif.org/issues/browse/CLB-69
    assertSearchMatch(1, "Trifolium repens", Rank.SPECIES);
    // Fucus http://dev.gbif.org/issues/browse/CLB-83
    assertSearchMatch(1, "Fucus( |$)", Rank.GENUS);
    assertClassification(1010512, "Fucaceae", "Fucales", "Phaeophyceae", "Ochrophyta", "Chromista");
    // bird Zonotrichia albicollis http://dev.gbif.org/issues/browse/CLB-119
    assertUsage(5231140, Rank.SPECIES, "Zonotrichia albicollis (Gmelin, 1789)", true, Kingdom.ANIMALIA);
    assertClassification(5231140, "Zonotrichia", "Emberizidae", "Passeriformes", "Aves", "Chordata", "Animalia");

    LOG.info("Test inter rank homonyms");
    // http://dev.gbif.org/issues/browse/CLB-63
    assertSearchMatch(2, "Lobata");
    assertSearchMatch(2, "Coccoidea");
    // 2 genera, 1 class
    assertSearchMatch(3, "Acantharia");
    assertSearchMatch(1, "Acantharia", Rank.CLASS);
    assertSearchMatch(2, "Acantharia", Rank.GENUS);
    // the subphylum isnt part of the nub
    assertSearchMatch(1, "Radiolaria");

    LOG.info("Test duplicate names");
    // http://dev.gbif.org/issues/browse/CLB-126
    assertSearchMatch(1, "Orbicularia foveolata", Rank.SPECIES);
    assertUsage(6638224, Rank.SPECIES, "Orbicularia foveolata Britton", false, Kingdom.PLANTAE);
    assertSearchMatch(1, "Nymania insignis", Rank.SPECIES);
    assertUsage(6046058, Rank.SPECIES, "Nymania insignis K.Schum.", false, Kingdom.PLANTAE);
    assertSearchMatch(1, "Phyllanthodendron lingulatum", Rank.SPECIES);
    assertSearchMatch(1, "Acidoton flueggeoides", Rank.SPECIES);
    assertSearchMatch(1, "Villanova buxifolia", Rank.SPECIES);
    assertSearchMatch(1, "Colmeiroa buxifolia", Rank.SPECIES);
    assertSearchMatch(1, "Maschalanthus obovatus", Rank.SPECIES);
    // homonym genus
    assertSearchMatch(3, "Wielandia", Rank.GENUS);
    assertSearchMatch(1, "Wielandia danguyana", Rank.SPECIES);

    LOG.info("Test fungal names");
    assertUsage(6348821, Rank.VARIETY, "Hendersonia sarmentorum var. catalpae Sandu", true, Kingdom.FUNGI);
    assertUsage(6332962, Rank.VARIETY, "Ithyphallus aurantiacus var. gracilis E. Fisch.", true, Kingdom.FUNGI);
    assertUsage(6336702, Rank.VARIETY, "Microsphaera alphitoides var. chenii U. Braun", true, Kingdom.FUNGI);
    assertUsage(6342638, Rank.VARIETY, "Peltidea canina var. glabra Ach.", true, Kingdom.FUNGI);
    assertUsage(6774914, Rank.VARIETY, "Placodium boergesenii var. squamosoareolata Vain.", true, Kingdom.FUNGI);
    assertUsage(6329816, Rank.VARIETY, "Togaria aurea var. aurea (Matt.) W.G. Sm.", true, Kingdom.FUNGI);
    assertUsage(6340009, Rank.VARIETY, "Nesolechia xenophana var. hazslinszkyana Keissl.", true, Kingdom.FUNGI);
    assertUsage(6328015, Rank.VARIETY, "Omphalia lignatilis var. albovirens QuÃ©l.", true, Kingdom.FUNGI);
    //ID had changed before, why?
    assertUsage(7252140, Rank.SPECIES, "Briarea orbicula (Corda) Bonord.", true, Kingdom.FUNGI);
    assertUsage(6012748, Rank.FORM, "Echinoderma acutesquamosum f. giganteum (PilÃ¡t) Bon", true, Kingdom.FUNGI);

    //instable autonym ID: http://dev.gbif.org/issues/browse/CLB-183
    LOG.info("Autonym ids");
    assertUsage(7243638, Rank.VARIETY, "Drosophila squamosa var. squamosa", true, Kingdom.FUNGI);
    // animal homonym genus
    assertUsage(6330670, Rank.VARIETY, "Drosophila caput-medusae var. caput-medusae (Fr.) KÃ¼hner & Romagn.", true, Kingdom.FUNGI);
    assertUsage(6350412, Rank.VARIETY, "Parasterina grewiae var. grewiae (Cooke) Bat. & Maia", true, Kingdom.FUNGI);

    LOG.info("Test mollusc names");
    assertUsage(4366023, Rank.VARIETY, "Bolinus brandaris var. adunca Coen, 1933", false, Kingdom.ANIMALIA);
    assertClassification(4366023, "Bolinus brandaris", "Bolinus", "Muricidae", "Neogastropoda", "Gastropoda", "Mollusca", "Animalia");
    assertUsage(5726859, Rank.VARIETY, "Murex brandaris var. aculeatus Philippi, 1836", false, Kingdom.ANIMALIA);
    assertUsage(4371850, Rank.VARIETY, "Ensis arcuatus var. directus", false, Kingdom.ANIMALIA);

    LOG.info("Test insect names");
    assertUsage(6097478, Rank.VARIETY, "Atheta elongatula var. balcanica", true, Kingdom.ANIMALIA);
    assertClassification(6097478, "Atheta elongatula", "Atheta", "Staphylinidae", "Staphylinoidea", "Coleoptera", "Insecta", "Arthropoda", "Animalia");

    LOG.info("Test Asteraceae");
    assertSearchMatch(1, "Asteraceae");
    assertUsage(3065, Rank.FAMILY, "Asteraceae Bercht. & J.Presl", true, Kingdom.PLANTAE);

    LOG.info("Nub verified!");
  }

  private void assertParentsContain(String searchName, Rank searchRank, String parent) {
    try (Transaction tx = db.beginTx()) {
      Node start = findUsageByCanonical(searchName, searchRank).node;
      assertParentsContain(start, null, parent);
    } catch (Exception e) {
      valid = false;
      LOG.error("Classification for {} {} lacks parent {}", searchRank, searchName, parent, e);
    }
  }

  private void assertParentsContain(Integer usageKey, Rank parentRank, String parent) {
    try (Transaction tx = db.beginTx()) {
      Node start = db.dao.getNeo().getNodeById(usage2NubKey.get(usageKey));
      assertParentsContain(start, parentRank, parent);
    } catch (Exception e) {
      valid = false;
      LOG.error("Classification for usage {} missing {}", usageKey, parent, e);
    }
  }

  private void assertParentsContain(Node start, @Nullable Rank parentRank, String parent) throws AssertionError {
    try (Transaction tx = db.beginTx()) {
      boolean found = false;
      for (Node p : Traversals.PARENTS.traverse(start).nodes()) {
        NubUsage u = db.dao.readNub(p);
        if (parent.equalsIgnoreCase(u.parsedName.canonicalName()) && (parentRank == null || u.rank.equals(parentRank))) {
          found = true;
        }
      }
      assertTrue(found);
    }
  }

  private void assertClassification(Integer usageKey, String... classification) {
    Iterator<String> expected = Lists.newArrayList(classification).iterator();
    try (Transaction tx = db.beginTx()) {
      Node start = db.dao.getNeo().getNodeById(usage2NubKey.get(usageKey));
      for (Node p : Traversals.PARENTS.traverse(start).nodes()) {
        NubUsage u = db.dao.readNub(p);
        assertEquals(expected.next(), u.parsedName.canonicalName());
      }
      assertFalse(expected.hasNext());
    } catch (Exception e) {
      valid = false;
      LOG.error("Classification for usage {} wrong", usageKey, e);
    }
  }

  private void assertSearchMatch(int expectedSearchMatches, String name) {
    assertSearchMatch(expectedSearchMatches, name, null);
  }

  private void assertSearchMatch(int expectedSearchMatches, String name, Rank rank) {
    List<NubUsage> matches = Lists.newArrayList();
    try {
      matches = findUsagesByCanonical(name, rank);
      assertEquals(expectedSearchMatches, matches.size());
    } catch (Exception e) {
      valid = false;
      LOG.error("Expected {} matches, but found {} for name {} with rank {}", expectedSearchMatches, matches.size(), name, rank);
    }
  }

  private NubUsage assertUsage(int usageKey, Rank rank, String name, boolean isAccepted, Kingdom kingdom) {
    NubUsage u = null;
    try (Transaction tx = db.beginTx()) {
      long nodeId = usage2NubKey.get(usageKey);
      u = db.dao.readNub(nodeId);
      assertNotNull(u);
      assertEquals(rank, u.rank);
      assertTrue(u.parsedName.canonicalNameComplete().startsWith(name));
      assertEquals(isAccepted, u.status.isAccepted());
      Integer kid = findRootUsageKey(u);
      if (kingdom == null) {
        assertNull(kid);
      } else {
        assertEquals(kingdom.nubUsageID(), kid);
      }
    } catch (Exception e) {
      LOG.error("Usage {}, {} wrong: {}", usageKey, name, e);
      valid = false;
    }
    return u;
  }




  private int findRootUsageKey(NubUsage u) {
    try (Transaction tx = db.beginTx()) {
      Node root = IteratorUtil.last(Traversals.PARENTS.traverse(u.node).nodes());
      return db.dao.readNub(root).usageKey;
    }
  }

  private NubUsage findUsageByCanonical(String name, Rank rank) {
    List<NubUsage> matches = findUsagesByCanonical(name, rank);
    if (matches.size() != 1) {
      valid = false;
      LOG.error("{} matches when expecting single match for {} {}", matches.size(), rank, name);
    }
    return matches.get(0);
  }

  private List<NubUsage> findUsagesByCanonical(String name, @Nullable Rank rank) {
    List<NubUsage> matches = Lists.newArrayList();
    try (Transaction tx = db.beginTx()) {
      for (Node n : db.dao.findByName(name)) {
        NubUsage u = db.dao.readNub(n);
        if (rank == null || rank.equals(u.rank)) {
          matches.add(u);
        }
      }
    }
    return matches;
  }

}
