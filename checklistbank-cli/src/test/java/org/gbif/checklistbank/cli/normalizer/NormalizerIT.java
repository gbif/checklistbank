package org.gbif.checklistbank.cli.normalizer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Main integration tests for the normalizer testing imports of entire small checklists.
 */
public class NormalizerIT extends NeoTest {
    private static final String INCERTAE_SEDIS = "Incertae sedis";
    private NormalizerConfiguration cfg;

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
        NormalizerStats stats = norm.run();
        System.out.println(stats);

        assertEquals(20, stats.getRecords());
        assertEquals(20, stats.getUsages());
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
    @Ignore("denormalizing checklists not yet working")
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
        assertEquals(20, stats.getRecords());
        assertEquals(20, stats.getUsages());
        assertEquals(6, stats.getDepth());
        assertEquals(20, stats.getCountByOrigin(Origin.SOURCE));
        assertEquals(1, stats.getRoots());
        assertEquals(4, stats.getSynonyms());
    }


    @Test
    @Ignore
    public void testSynonymsWIthMissingAccepted() throws Exception {
        NormalizerStats stats = normalize(3);
        try (Transaction tx = beginTx()) {

            //Coleoptera
            NameUsage coleo = getUsageByTaxonId("4");

            // Pygoleptura nigrella
            NameUsage syn = getUsageByTaxonId("200");
            assertTrue(syn.isSynonym());

            // test for inserted incertae sedis usage
            NameUsage incertae = getUsageByKey(syn.getParentKey());
            assertEquals(INCERTAE_SEDIS, incertae.getScientificName());
            assertFalse(incertae.isSynonym());
            // make sure synonym taxonomy is preserved in incertae sedis
            assertEquals(coleo.getKey(), incertae.getParentKey());

            // make sure synonyms of synonyms are relinked to incertae accepted
            // Leptura nigrella Adams, 1909
            NameUsage synLep = getUsageByTaxonId("100");
            assertTrue(synLep.isSynonym());
            assertEquals(incertae.getKey(), synLep.getParentKey());
            // Leptura nigrella Chagnon, 1917
            synLep = getUsageByTaxonId("101");
            assertTrue(synLep.isSynonym());
            assertEquals(incertae.getKey(), synLep.getParentKey());
        }
    }

    @Test
    @Ignore
    public void testColSynonyms() throws Exception {
        NormalizerStats stats = normalize(4);
        try (Transaction tx = beginTx()) {

            // Phoenicus sanguinipennis
            NameUsage acc = getUsageByTaxonId("248320");
            assertFalse(acc.isSynonym());

            NameUsage syn = getUsageByTaxonId("282664");
            assertTrue(syn.isSynonym());

            assertEquals(acc.getKey(), syn.getParentKey());

            // Anniella pulchra pulchra
            NameUsage species = getUsageByTaxonId("1938166");
            assertFalse(species.isSynonym());

            NameUsage speciesSyn = getUsageByTaxonId("1954175");
            assertTrue(speciesSyn.isSynonym());

            NameUsage ssppulchra = getUsageByTaxonId("1943002");
            assertFalse(ssppulchra.isSynonym());
            assertEquals(ssppulchra.getKey(), speciesSyn.getParentKey());
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
    @Ignore
    public void testIncertaeSedisSynonyms() throws Exception {
        NormalizerStats stats = normalize(5);
        try (Transaction tx = beginTx()) {
            // Tubiferidae Cossmann, 1895
            // "mol101988","Tubiferidae Cossmann, 1895","Cossmann, 1895",,,"Tubiferidae","Heterostropha","Gastropoda","Mollusca","Animalia","family","synonym"
            NameUsage syn = getUsageByTaxonId("mol101988");
            assertTrue(syn.isSynonym());

            NameUsage sedis = getUsageByKey(syn.getParentKey());
            assertFalse(sedis.isSynonym());
            assertEquals(INCERTAE_SEDIS, sedis.getScientificName());

            NameUsage parent = getUsageByKey(sedis.getParentKey());
            assertFalse(parent.isSynonym());
            assertEquals("Heterostropha", parent.getScientificName());
            assertEquals(Rank.ORDER, parent.getRank());

            // Acanthophora Hulst, 1896
            syn = getUsageByTaxonId("hex1088048");
            assertTrue(syn.isSynonym());

            NameUsage acc = getUsageByKey(syn.getParentKey());
            assertFalse(acc.isSynonym());
            assertEquals("Acanthotoca", acc.getScientificName());

            parent = getUsageByKey(acc.getParentKey());
            assertFalse(parent.isSynonym());
            assertEquals("Geometridae", parent.getScientificName());
            assertEquals(Rank.FAMILY, parent.getRank());

            // Acanthophora Borgmeier, 1922
            syn = getUsageByTaxonId("hex1090241");
            assertTrue(syn.isSynonym());

            acc = getUsageByKey(syn.getParentKey());
            assertFalse(acc.isSynonym());
            assertEquals("Acanthophorides", acc.getScientificName());

            parent = getUsageByKey(acc.getParentKey());
            assertFalse(parent.isSynonym());
            assertEquals("Phoridae", parent.getScientificName());
            assertEquals(Rank.FAMILY, parent.getRank());
        }
    }

    /**
     * Tests the index fungorum format using a denormed classification.
     * All records of the genus Zignoëlla have been included in the test resources,
     * as only when all are present the original issue of missing higher taxa shows up.
     * <p/>
     * The genus field is left out in the meta.xml, as it causes confusion when the genus is regarded as a synonyms,
     * but there are species within that genus still being accepted. An oddity of the nomenclatoral index fungorum database.
     * Discovered with this test.
     */
    @Test
    @Ignore
    public void testDenormedIndexFungorum() throws Exception {
        NormalizerStats stats = normalize(6);
        try (Transaction tx = beginTx()) {
            assertUsage("426221", false, "Lepiota seminuda var. seminuda (Lasch) P. Kummer",
                "Lepiota seminuda var. seminuda (Lasch) P. Kummer", Rank.VARIETY, "Agaricaceae", "Agaricales", "Agaricomycetes", "Basidiomycota", "Fungi");

            assertUsage("140283", true, "Polystictus substipitatus (Murrill) Sacc. & Trotter",
                "Coriolus substipitatus Murrill", Rank.SPECIES, "Trametes modesta (Kunze ex Fr.) Ryvarden", "Polyporaceae", "Polyporales", "Agaricomycetes", "Basidiomycota", "Fungi");

            assertUsage("233484", false, "Zignoëlla culmicola Delacr.",
                "Zignoëlla culmicola Delacr.", Rank.SPECIES, "Chaetosphaeriaceae", "Chaetosphaeriales", "Sordariomycetes", "Ascomycota", "Fungi");

            assertUsage("970", false, "Chaetosphaeria Tul. & C. Tul.",
                "Chaetosphaeria Tul. & C. Tul.", Rank.GENUS, "Chaetosphaeriaceae", "Chaetosphaeriales", "Sordariomycetes", "Ascomycota", "Fungi");
        }
    }


    /**
     * Tests the creation of parent and accepted usages given as verbatim names via acceptedNameUsage or parentNameUsage.
     */
    @Test
    @Ignore("test outcomes not yet migrated")
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

            u = getUsageByKey(u.getParentKey());
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
            assertTrue(u.isSynonym());
            assertEquals(Rank.SUBSPECIES, u.getRank());
            assertEquals("Pygoleptura tinktura subsp. synomica Döring, 2011", u.getScientificName());

            u = getUsageByKey(u.getParentKey());
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
            assertEquals(17, stats.getUsages());
            assertEquals(1, stats.getRoots());

            // genus synonym
            NameUsage nu = null; // = usageService.search(null, null, "Cladendula Döring", SearchType.fullname, 110, null, null, null, null, null, null).get(0);
            assertEquals("Cladendula Döring", nu.getScientificName());
            assertEquals(Rank.GENUS, nu.getRank());
            assertEquals(TaxonomicStatus.SYNONYM, nu.getTaxonomicStatus());
            assertTrue(nu.isSynonym() == true);

            NameUsage acc = getUsageByKey(nu.getParentKey());
            assertEquals("Calendula L.", acc.getScientificName());
            assertEquals(Rank.GENUS, acc.getRank());
            assertEquals(TaxonomicStatus.ACCEPTED, acc.getTaxonomicStatus());
            assertTrue(acc.isSynonym() == false);

            // pro parte synonym
            List<NameUsage> propartes = Lists.newArrayList(); // = usageService.search(null, null, "Calendula eckerleinii Ohle", SearchType.fullname, 110, null, null, null, null, null, null);
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

            //the synonym cycle should be cut, so not all ids exist as accepted
            Set<Integer> accIds = Sets.newHashSet();
            for (Integer id : new Integer[]{10002, 10003, 10004}) {
                Integer accId = getUsageByTaxonId(id.toString()).getAcceptedKey();
                if (accId != null) {
                    accIds.add(accId);
                }
            }
            assertTrue("Synonym cycle not cut", accIds.size() < 3);
        }
    }

    public static UUID datasetKey(Integer x) throws NormalizationFailedException {
        return UUID.fromString(String.format("%08d-c6af-11e2-9b88-00145eb45e9a", x));
    }

    private NormalizerStats normalize(Integer dKey) throws NormalizationFailedException {
        UUID datasetKey = datasetKey(dKey);
        Normalizer norm = Normalizer.build(cfg, datasetKey, null);
        NormalizerStats stats = norm.run();

        System.out.println(stats);
        initDb(datasetKey);
        return stats;
    }
}