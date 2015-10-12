package org.gbif.checklistbank.nub;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NodeProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.StartEndHandler;
import org.gbif.checklistbank.neo.traverse.TaxonWalker;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.neo.traverse.TreePrinter;
import org.gbif.checklistbank.nub.lookup.IdLookupImpl;
import org.gbif.checklistbank.nub.lookup.LookupUsage;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.source.ClasspathSourceList;
import org.gbif.checklistbank.nub.source.NubSourceList;
import org.gbif.checklistbank.nub.source.RandomSource;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NubBuilderIT {
    private UsageDao dao;
    private Transaction tx;

    @Before
    public void init() {
        dao = UsageDao.temporaryDao(128);
    }

    @After
    public void shutdown() {
        if (tx != null) {
            tx.close();
        }
        dao.closeAndDelete();
    }

    @Test
    public void testKingdoms() throws Exception {
        build(ClasspathSourceList.emptySource());
        // bad ranks, no source usages should be created
        assertEquals(Kingdom.values().length, countTaxa());
        for (Kingdom k : Kingdom.values()) {
            NubUsage u = getCanonical(k.scientificName(), Rank.KINGDOM);
            assertEquals(k.scientificName(), u.parsedName.canonicalNameComplete());
            assertEquals(k.nubUsageID(), (Integer) u.usageKey);

            NameUsage nu = getUsage(u.node);
            assertEquals(nu.getScientificName(), u.parsedName.canonicalNameComplete());
            assertEquals(nu.getKey(), (Integer) u.usageKey);
        }
    }

    /**
     * The GBIF backbone only accepts mayor linnean ranks above species level.
     * For infraspecific names we only accept subspecies in zoology, but other ranks can treated as synonyms.
     * In botany subspecies, variety or form is an accepted rank.
     */
    @Test
    public void testBackboneRanks() throws Exception {
        build(ClasspathSourceList.source(1));

        // bad ranks, no source usages should be created
        assertTrue(listCanonical("Lepiota nuda maxima").isEmpty());
        assertTrue(listCanonical("Agaricaceaes").isEmpty());
        assertTrue(listCanonical("Francisella tularensis rosensis").isEmpty());
        assertTrue(listCanonical("Francisella tularensis tularensis").isEmpty());
    }

    @Test
    public void testUnknownKingdom() throws Exception {
        build(ClasspathSourceList.source(4));

        NubUsage k = assertCanonical(Kingdom.INCERTAE_SEDIS.scientificName(), Rank.KINGDOM, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
        NubUsage f = assertCanonical("Popeliaceae", Rank.FAMILY, Origin.SOURCE, TaxonomicStatus.ACCEPTED, k);
        NubUsage g = assertCanonical("Lepiota", Rank.GENUS, Origin.IMPLICIT_NAME, TaxonomicStatus.ACCEPTED, f);
        NubUsage u = assertCanonical("Lepiota nuda", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, g);
    }

    @Test
    public void testUpdateAuthorship() throws Exception {
        build(ClasspathSourceList.source(1, 5, 6));

        assertCanonical("Agaricaceae", "Yoda", Rank.FAMILY, Origin.SOURCE);
        assertCanonical("Lepiota seminuda", "Miller", Rank.SPECIES, Origin.SOURCE);
        assertCanonical("Lepiota nuda elegans", "DC.", Rank.SUBSPECIES, Origin.SOURCE);
        assertCanonical("Lepiota nuda nuda", "", Rank.SUBSPECIES, Origin.AUTONYM);
        assertCanonical("Lepiota nuda europaea", "Döring", Rank.VARIETY, Origin.SOURCE);

        assertTree("1 5 6.txt");
    }

    /**
     * http://dev.gbif.org/issues/browse/POR-398
     */
    @Test
    public void testMergeBasionymGroup() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(25, 26);
        build(src);

        NubUsage spec = getCanonical("Picris hieracioides", Rank.SPECIES);
        NubUsage umbella = assertCanonical("Picris hieracioides umbellata", Rank.SUBSPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, spec);
        NubUsage hieracio = assertCanonical("Picris hieracioides hieracioides", Rank.SUBSPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, spec);

        NubUsage phu = assertCanonical("Picris hieracioides umbellata", Rank.VARIETY, Origin.SOURCE, TaxonomicStatus.HOMOTYPIC_SYNONYM, umbella);
        NameUsage u = getUsage(phu.node);
        assertEquals("Leontodon umbellatus Schrank", u.getBasionym());

        assertCanonical("Picris sonchoides", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.HETEROTYPIC_SYNONYM, umbella);
        assertCanonical("Leontodon umbellatus", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.HOMOTYPIC_SYNONYM, umbella);
        assertCanonical("Apargia umbellata", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.HOMOTYPIC_SYNONYM, umbella);
        assertCanonical("Picris umbellata", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.HOMOTYPIC_SYNONYM, umbella);
        assertCanonical("Picris hieracioides sonchoides", Rank.SUBSPECIES, Origin.SOURCE, TaxonomicStatus.SYNONYM, umbella);
    }

    /**
     * http://dev.gbif.org/issues/browse/POR-284
     * 4 name pairs each with a diacretic version should result in just 4 distinct nub names.
     */
    @Test
    public void testDiacriticNames() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(27);
        build(src);

        assertEquals(4, countSpecies());
    }

    /**
     * Make sure explicit basionym i.e. original name usage relations make it into the backbone.
     * Dataset 21 contains a conflicting basionym for Martes martes, make sure we use the preferred source dataset 20.
     */
    @Test
    public void testExplicitBasionyms() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(20, 21);
        build(src);

        assertEquals(1, IteratorUtil.asList(getCanonical("Mustela martes", Rank.SPECIES).node.getRelationships(RelType.BASIONYM_OF)).size());
        assertEquals(1, IteratorUtil.asList(getCanonical("Martes martes", Rank.SPECIES).node.getRelationships(RelType.BASIONYM_OF)).size());

        NameUsage u = getUsage(getCanonical("Martes martes", Rank.SPECIES).node);
        assertEquals("Mustela martes Linnaeus, 1758", u.getBasionym());

        u = getUsage(getCanonical("Martes markusis", Rank.SPECIES).node);
        assertEquals("Cellophania markusa Döring, 2001", u.getBasionym());

        u = getUsage(getCanonical("Cellophania markusa", Rank.SPECIES).node);
        assertNull(u.getBasionym());
    }

    /**
     * http://dev.gbif.org/issues/browse/POR-2786
     */
    @Test
    public void testStableIds() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(3, 2);
        src.setSourceRank(3, Rank.KINGDOM);
        build(src);

        int o1 = getScientific("Oenanthe Vieillot, 1816", Rank.GENUS).usageKey;
        int o2 = getScientific("Oenanthe Linnaeus, 1753", Rank.GENUS).usageKey;

        int t1 = getScientific("Trichoneura bontocensis Alexander, 1934", Rank.SPECIES).usageKey;
        int t2 = getScientific("Trichoneura hirtella Napper", Rank.SPECIES).usageKey;
        int t1p = parentOrAccepted(getScientific("Trichoneura bontocensis Alexander, 1934", Rank.SPECIES).node).usageKey;
        int t2p = parentOrAccepted(getScientific("Trichoneura hirtella Napper", Rank.SPECIES).node).usageKey;

        int b1 = getScientific("Blattaria P. Miller, 1754", Rank.GENUS).usageKey;
        int b2 = getScientific("Blattaria O. Kuntze, 1891", Rank.GENUS).usageKey;
        int b3 = getScientific("Blattaria Voet, 1806", Rank.GENUS).usageKey;
        int b4 = getScientific("Blattaria Weyenbergh, 1874", Rank.GENUS).usageKey;

        // rebuild nub with additional sources!
        src = ClasspathSourceList.source(3, 2, 8, 11);
        src.setSourceRank(3, Rank.KINGDOM);
        rebuild(src);

        assertTree("3 2 8 11.txt");

        // assert ids havent changed!
        assertEquals(o1, getScientific("Oenanthe Vieillot, 1816", Rank.GENUS).usageKey);
        assertEquals(o2, getScientific("Oenanthe Linnaeus, 1753", Rank.GENUS).usageKey);

        assertEquals(t1, getScientific("Trichoneura bontocensis Alexander, 1934", Rank.SPECIES).usageKey);
        assertEquals(t2, getScientific("Trichoneura hirtella Napper", Rank.SPECIES).usageKey);
        assertEquals(t1p, parentOrAccepted(getScientific("Trichoneura bontocensis Alexander, 1934", Rank.SPECIES).node).usageKey);
        assertEquals(t2p, parentOrAccepted(getScientific("Trichoneura hirtella Napper", Rank.SPECIES).node).usageKey);

        assertEquals(b1, getScientific("Blattaria P. Miller, 1754", Rank.GENUS).usageKey);
        assertEquals(b2, getScientific("Blattaria O. Kuntze, 1891", Rank.GENUS).usageKey);
        assertEquals(b3, getScientific("Blattaria Voet, 1806", Rank.GENUS).usageKey);
        assertEquals(b4, getScientific("Blattaria Weyenbergh, 1874", Rank.GENUS).usageKey);
    }

    @Test
    public void testUpdateClassification() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(3, 5, 7);
        src.setSourceRank(3, Rank.KINGDOM);
        build(src);

        NubUsage fam = assertCanonical("Agaricaceae", "Yoda", Rank.FAMILY, Origin.SOURCE);
        NubUsage g = assertCanonical("Lepiota", Rank.GENUS, Origin.IMPLICIT_NAME, fam);
        NubUsage ls = assertCanonical("Lepiota seminuda", Rank.SPECIES, Origin.SOURCE, g);

        assertClassification(ls, "Lepiota", "Agaricaceae", "Agaricales", "Agaricomycetes", "Basidiomycota", "Fungi");

        // this genus should not be updated as its classification in source 7 contradicts the original one
        NubUsage b = assertCanonical("Berto", "Miller", Rank.GENUS, Origin.SOURCE);
        assertClassification(b, "Agaricales", "Agaricomycetes", "Basidiomycota", "Fungi");
    }

    @Test
    public void testCreateImplicitGenus() throws Exception {
        build(ClasspathSourceList.source(1));

        NubUsage genusF = assertCanonical("Lepiota", Rank.GENUS, Origin.IMPLICIT_NAME, Kingdom.FUNGI, null);
        assertCanonical("Lepiota seminuda", Rank.SPECIES, Origin.SOURCE, genusF);
        final NubUsage species = assertCanonical("Lepiota nuda", Rank.SPECIES, Origin.IMPLICIT_NAME, genusF);
        assertCanonical("Lepiota nuda elegans", Rank.SUBSPECIES, Origin.SOURCE, species);
        assertCanonical("Lepiota nuda europaea", Rank.VARIETY, Origin.SOURCE, species);

        NubUsage genusA = assertCanonical("Lepiota", Rank.GENUS, Origin.IMPLICIT_NAME, Kingdom.ANIMALIA, null);
        assertCanonical("Lepiota carlanova", "MD", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, genusA);
    }

    /**
     * Accepted infraspecific names must have a corresponding autonym in both the zoological and botanical code.
     * In zoology we only accept infraspecies taxa at the rank of subspecies.
     * In botany it can be at subspecies, variety or form level.
     * For synonyms autonyms are NOT generated.
     */
    @Test
    public void testCreateImplicitAutonym() throws Exception {
        build(ClasspathSourceList.source(1));

        List<NubUsage> nudas = listCanonical("Lepiota nuda nuda");
        assertEquals(2, nudas.size());

        NubUsage var = null;
        NubUsage ssp = null;
        for (NubUsage u : nudas) {
            assertEquals("Lepiota nuda nuda", u.parsedName.getScientificName());
            assertEquals(Origin.AUTONYM, u.origin);
            if (u.rank == Rank.SUBSPECIES) {
                ssp = u;
            } else if (u.rank == Rank.VARIETY) {
                var = u;
            } else {
                fail("bad rank");
            }
        }
        assertEquals(Rank.SUBSPECIES, ssp.rank);
        assertEquals(Rank.VARIETY, var.rank);

        // bad ranks, no autonym should be created
        assertTrue(listCanonical("Lepiota nuda maxima").isEmpty());
        assertNull(getCanonical("Lepiota nuda nuda", Rank.SUBVARIETY));
    }

    @Test
    public void testHigherClassification() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(3);
        src.setSourceRank(3, Rank.KINGDOM);
        build(src);

        assertNotNull(getCanonical("Animalia", Rank.KINGDOM));
        assertNotNull(getCanonical("Coleoptera", Rank.ORDER));
        assertNotNull(getCanonical("Poaceae", Rank.FAMILY));
    }

    /**
     * Make sure that species not snapping to any existing higher taxon get created under the incertae sedis kingdom
     */
    @Test
    public void testIncertaeSedis() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(34);
        build(src);

        assertTree("34.txt");
    }

    @Test
    public void testColAdiantumSynonym() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(8);
        src.setSourceRank(8, Rank.PHYLUM);
        build(src);

        assertTree("8.txt");

        List<NubUsage> pedatums = listCanonical("Adiantum pedatum");
        assertEquals(4, pedatums.size());
        for (NubUsage u : pedatums) {
            System.out.println(u.parsedName.getScientificName());
            NubUsage p = parentOrAccepted(u.node);
            switch (u.parsedName.getScientificName()) {
                case "Adiantum pedatum":
                    assertFalse(u.status.isSynonym());
                    assertEquals("Adiantum", p.parsedName.canonicalName());
                    break;
                case "Adiantum pedatum Forst.":
                    assertTrue(u.status.isSynonym());
                    assertFalse(p.status.isSynonym());
                    assertEquals("Adiantum hispidulum Sw.", p.parsedName.getScientificName());
                    break;
                case "Adiantum pedatum A. Peter":
                    assertTrue(u.status.isSynonym());
                    assertFalse(p.status.isSynonym());
                    assertEquals("Adiantum patens subsp. oatesii (Bak.) Schelpe", p.parsedName.getScientificName());
                    break;
                case "Adiantum pedatum Raddi":
                    assertTrue(u.status.isSynonym());
                    assertFalse(p.status.isSynonym());
                    assertEquals("Adiantum brasiliense Raddi", p.parsedName.getScientificName());
                    break;
                default:
                    fail("Unexpected name " + u.parsedName.getScientificName());
            }
        }
    }

    /**
     * An accepted species with a genus that the nub already considers as a synonym should not be accepted.
     * Try to combine the epithet to the accepted genus and if its a new name make it doubtful until we hit another source with that name.
     */
    @Test
    public void testSpeciesInSynonymGenus() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(11, 12);
        build(src);

        NubUsage oct = assertCanonical("Octopus", "Cuvier, 1797", null, Rank.GENUS, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
        NubUsage amph = assertCanonical("Amphioctopus", "Fischer, 1882", null, Rank.GENUS, Origin.SOURCE, TaxonomicStatus.SYNONYM, oct);

        assertCanonical("Octopus vulgaris", "Cuvier, 1797", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, oct);
        // not the same year, so basionym grouping not applied
        assertCanonical("Octopus fangsiao", "d'Orbigny, 1839", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, oct);
        assertCanonical("Amphioctopus fangsiao", "(d'Orbigny, 1835)", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, oct, NameUsageIssue.NAME_PARENT_MISMATCH);

        assertNull(getCanonical("Octopus markus", Rank.SPECIES));
        // accepted species becomes doubtful with issue and parent Octopus
        assertCanonical("Amphioctopus markus", "Döring, 1999", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, oct, NameUsageIssue.NAME_PARENT_MISMATCH);
    }

    /**
     * The genus name Oenanthe is used as
     * 1. accepted bird genus Oenanthe Vieillot, 1816
     * 2. accepted plant genus Oenanthe Linnaeus, 1753
     * Similar the genus Trichoneura:
     * http://www.catalogueoflife.org/col/search/all/key/Trichoneura/match/1
     * The genus Blattaria exists 4 times in IRMNG:
     * 1. Blattaria P. Miller, 1754  [Scrophulariaceae]
     * 2. Blattaria O. Kuntze, 1891  [Malvaceae] SYN of Pentapetes Linnaeus 1753
     * 3. Blattaria Voet, 1806  [Coleoptera]
     * 4. Blattaria Weyenbergh, 1874  [Orthoptera fossil]
     * Blattaria only exists as synonym species names in CoL.
     * Should there be any accepted genus at all in GBIF?
     *
     * Also test what happens if a higher taxon exists twice with a slightly different classification in CoL.
     * E.g. class Jungermanniopsida
     *
     * Suggest to keep the first occurrence
     */
    @Test
    public void testHomonyms() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(3, 2);
        src.setSourceRank(3, Rank.KINGDOM);
        build(src);

        assertEquals(2, listCanonical("Oenanthe").size());

        assertEquals(2, listCanonical("Trichoneura").size());

        assertEquals(4, listCanonical("Blattaria").size());

        NubUsage march = assertCanonical("Marchantiophyta", null, Rank.PHYLUM, Origin.SOURCE);
        assertCanonical("Jungermanniopsida", Rank.CLASS, Origin.SOURCE, march);

        assertTree("3 2.txt");
    }

    @Test
    public void testGenusHomonyms() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(29, 30, 31);
        src.setSourceRank(29, Rank.PHYLUM);
        build(src);

        NubUsage pyro = assertCanonical("Pyrostephos", "Moser, 1925", null, Rank.GENUS, Origin.SOURCE);
        assertCanonical("Mica micula", "Margulis, 1982", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, pyro);
        assertCanonical("Mica spectata", "", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, pyro);

        List<NubUsage> micas = listCanonical("Mica");
        assertEquals(5, micas.size());

        assertTree("29 30 31.txt");
    }

    @Test
    public void testHybrids() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(9);
        src.setSourceRank(9, Rank.PHYLUM);
        build(src);

        assertNotNull(getCanonical("Plantae", Rank.KINGDOM));
        assertCanonical("Adiantum", "", null, Rank.GENUS, Origin.IMPLICIT_NAME);
        assertCanonical("Asplenium", "", null, Rank.GENUS, Origin.IMPLICIT_NAME);

        assertCanonical("Adiantum capillus-veneris", "L.", null, Rank.SPECIES, Origin.SOURCE);
        assertCanonical("Asplenium adiantum-nigrum", "", null, Rank.SPECIES, Origin.IMPLICIT_NAME);
        assertCanonical("Asplenium adiantum-nigrum yuanum", "(Ching) Viane, Rasbach, Reichstein & Schneller", null, Rank.SUBSPECIES, Origin.SOURCE);
        assertCanonical("Adiantum moranii", "J. Prado", NamePart.SPECIFIC, Rank.SPECIES, Origin.SOURCE);

        assertNull(getCanonical("Asplenium adiantum nigrum × septentrionale", Rank.SPECIES));
    }

    /**
     * As found in CoL as of june 2015. 4 times the same moss species name Fontinalis antipyretica with different authors, all accepted.
     * TODO: This is illegal to the code rules, so just one (or none?) should be accepted
     */
    @Test
    public void testMultipleAcceptedNames() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(10);
        build(src);

        NubUsage genus = assertCanonical("Fontinalis", "", null, Rank.GENUS, Origin.IMPLICIT_NAME);
        int counter = 0;
        for (NubUsage c : children(genus.node)) {
            assertEquals(Rank.SPECIES, c.rank);
            assertEquals("Fontinalis antipyretica", c.parsedName.canonicalName());
            counter++;
        }
        assertEquals(4, counter);
    }

    /**
     * Avoid seeing a stackoverflow error when trying to create missing genus or autonyms
     * with incomplete names missing a genus but that can be parsed.
     */
    @Test
    public void testIncompleteNames() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(24);
        src.setSourceRank(24, Rank.FAMILY);
        build(src);

        assertTrue(listCanonical("doliolum").isEmpty());
        assertTrue(listCanonical("Aster doliolum").isEmpty());
        assertTrue(listCanonical("Cichorium doliolum").isEmpty());
        assertTrue(listCanonical("Cichorium doliolum doliolum").isEmpty());

        assertTree("24.txt");
    }

    /**
     * CoL contains the genus Albizia twice within the plants as an accepted name (Fabaceae & Asteraceae).
     * http://www.catalogueoflife.org/col/details/species/id/17793647/source/tree
     * http://www.catalogueoflife.org/col/details/species/id/11468181/source/tree
     * The backbone should only contain one accepted genus in Fabaceae.
     * The Asteraceae one as doubtful.
     */
    @Test
    public void testAlbiziaCoL() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(13);
        src.setSourceRank(13, Rank.FAMILY);
        build(src);

        NubUsage fab = assertCanonical("Fabaceae", "", null, Rank.FAMILY, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
        NubUsage genus = assertCanonical("Albizia", "", null, Rank.GENUS, Origin.IMPLICIT_NAME, TaxonomicStatus.ACCEPTED, fab);
        NubUsage adianthifolia = assertCanonical("Albizia adianthifolia", "(Schum.) W.Wight", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, genus);

        assertCanonical("Albi minki", "W. Wight", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, null);
        NubUsage tomentosa = assertCanonical("Albi tomentosa", "(Micheli) Standl.", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);

        // these are recombinations from the Albizia names above and thus get converted into synonyms (not doubtful as sources suggest)
        assertCanonical("Albizia tomentosa", "(Micheli) Standl.", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.HOMOTYPIC_SYNONYM, tomentosa);
        assertCanonical("Albi adianthifolia", "(Schum.) W.Wight", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.HOMOTYPIC_SYNONYM, adianthifolia);

        assertTree("13.txt");
    }

    /**
     * CoL contains concept synonyms using the "sensu" notation in the ILDIS GSD (maybe elsewhere too).
     * http://dev.gbif.org/issues/browse/POR-389
     * See http://www.catalogueoflife.org/col/details/species/id/11468181/source/tree
     * Albizia adianthifolia (Schum.) W.Wight has the following synonyms:
     * Albizia gummifera sensu Capuron, p.p.
     * Albizia gummifera sensu R.O.Williams
     * Albizia sassa sensu Aubrev.
     * Mimosa adianthifolia Schum.
     * Albizia sassa is a synonym twice:
     * http://www.catalogueoflife.org/col/search/all/key/Albizia+sassa/match/1
     * Albizia gummifera is both accepted and a synonym when used as a concept:
     * http://www.catalogueoflife.org/col/search/all/key/Albizia+gummifera/match/1
     * Albizia gummifera (J.F.Gmel.) C.A.Sm. (ACC)
     * Albizia gummifera sensu Capuron, p.p. (SYN)
     * Albizia gummifera sensu R.O.Williams (SYN)
     */
    @Test
    public void testSecSynonyms() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(28);
        src.setSourceRank(28, Rank.FAMILY);
        build(src);

        NubUsage ast = assertCanonical("Asteraceae", "", null, Rank.FAMILY, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
        NubUsage alb = assertCanonical("Albizia", null, null, Rank.GENUS, Origin.IMPLICIT_NAME, TaxonomicStatus.ACCEPTED, ast);
        NubUsage gummi = assertCanonical("Albizia gummifera", "L.", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, alb);
        NubUsage adia = assertCanonical("Albizia adianthifolia", "(Schum.) W.Wight", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, alb);
        assertEquals(0, listSynonyms(adia).size());
    }

    /**
     * The same canonical name with a different author can be used as synonyms multiple times.
     * See http://dev.gbif.org/issues/browse/POR-353
     */
    @Test
    public void testSynonymsWithDifferentAuthors() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(14);
        build(src);

        // we should only have one accepted Geotrupes stercorarius as one name lacks the combination author!
        assertEquals(1, listCanonical("Geotrupes stercorarius").size());

        NubUsage gen = assertCanonical("Geotrupes", Rank.GENUS, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
        assertScientific("Geotrupes stercorarius (Linnaeus, 1758)", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, gen);
        assertNotExisting("Geotrupes stercorarius Erichson, 1847", Rank.SPECIES);

        assertCanonical("Geotrupes spiniger", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);

        assertEquals(2, listCanonical("Poa pubescens").size());
        gen = assertCanonical("Poa", Rank.GENUS, Origin.IMPLICIT_NAME, TaxonomicStatus.ACCEPTED, null);
        NubUsage acc = assertScientific("Poa pratensis L.", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, gen);
        assertScientific("Poa pubescens Lej.", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.SYNONYM, acc);

        gen = assertCanonical("Eragrostis", Rank.GENUS, Origin.IMPLICIT_NAME, TaxonomicStatus.ACCEPTED, null);
        acc = assertScientific("Eragrostis pubescens (R.Br.) Steud.", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, gen);
        assertScientific("Poa pubescens R.Br.", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.SYNONYM, acc);
    }

    /**
     * See http://dev.gbif.org/issues/browse/POR-325
     * Nitrospira is an accepted canonical name at various ranks:
     * 1. genus Nitrospira Watson et al., 1986
     * 2. class Nitrospira
     * 3. phylum Nitrospira
     * Vertebrata is both an accepted subphylum and a genus.
     * Lobata is a genus (algae) & an order (animal)
     * http://dev.gbif.org/issues/browse/POR-362
     */
    @Test
    public void testInterrankHomonyms() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(19);
        src.setSourceRank(19, Rank.PHYLUM);
        build(src);

        assertEquals(2, listCanonical("Archaea").size());
        assertEquals(3, listCanonical("Nitrospira").size());
        assertEquals(2, listCanonical("Lobata").size());
        // we ignore subphyla
        assertEquals(1, listCanonical("Vertebrata").size());
        assertEquals(1, listCanonical("Radiolaria").size());
    }

    /**
     * WoRMS contains some synonyms with the same canonical name that include the subgenus or not.
     * Make sure they all get merged into one synonym in the nub.
     * See http://www.marinespecies.org/aphia.php?p=taxdetails&id=191705
     */
    @Test
    public void testWormsSubgenusAlternateRepresentations() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(18);
        build(src);

        NubUsage gen = assertCanonical("Hyalonema", Rank.GENUS, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);

        // we dont have subgenera in the nub
        assertNotExisting("Hyalonema (Corynonema) Ijima, 1927", Rank.SUBGENUS);

        assertEquals(1, listCanonical("Hyalonema rotundum").size());
        NubUsage u = assertCanonical("Hyalonema rotundum", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, gen);

        assertTree("18.txt");
    }

    /**
     * - deal with unspecified infraspecific ranks in CoL
     * - IPNI names only linked to the familiy with no other hierarchy
     * - redundant IPNI names
     */
    @Test
    public void testColAndIpni() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(32, 33);
        src.setSourceRank(32, Rank.PHYLUM);
        src.setNomenclator(33);
        build(src);

        assertTree("32 33.txt");
    }

    @Test
    @Ignore("Manual test for profiling performance issues")
    public void testPerformance() throws Exception {
        NubSourceList src = new NubSourceList(Lists.newArrayList(
                new RandomSource(200000, Kingdom.ANIMALIA),
                new RandomSource(100, Kingdom.BACTERIA),
                new RandomSource(100, Kingdom.ARCHAEA),
                new RandomSource(20000, Kingdom.FUNGI),
                new RandomSource(50000, Kingdom.PLANTAE)));
        build(src);
    }

    /**
     * CoL contains canonical synonyms without authorship which are linked to an accepted taxon which has the same canonical name, but includes proper authorship.
     * Ignore those synonyms as they are meaningless and cluttering.
     */
    @Test
    public void testAvoidCanonicalSynonym() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(17);
        build(src);
        assertEquals(1, listCanonical("Fuligo septica").size());
    }

    /**
     * Pro parte synonyms should exist as a single synonym node with multiple synonym relations
     */
    @Test
    public void testProParteSynonym() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(15, 16);
        build(src);

        NubUsage u = assertCanonical("Poa pubescens", "Lej.", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.PROPARTE_SYNONYM, null);
        assertEquals(3, u.sourceIds.size());
        List<Relationship> rels = IteratorUtil.asList(u.node.getRelationships(RelType.PROPARTE_SYNONYM_OF, Direction.OUTGOING));
        Relationship acc = IteratorUtil.single(u.node.getRelationships(RelType.SYNONYM_OF, Direction.OUTGOING));
        assertEquals(1, rels.size());
        assertNotEquals(rels.get(0).getEndNode(), acc.getEndNode());

        assertTree("15 16.txt");
    }

    /**
     * Prefer a nomenclator name and nom status over any other sources!
     * Prefer a name with authorship over a bare canonical one.
     * Use a nomenclators taxonID as scientificNameID for the nub usage.
     */
    @Test
    @Ignore("write test")
    public void testUpdateNameString() throws Exception {

    }

    /**
     * Test merging of taxon classification from different sources.
     * Only merge the classification when taxonomic status is the same.
     * For synonyms use the accepted name from later sources in case the primary one is incertae-sedis.
     */
    @Test
    @Ignore("write test")
    public void testMergingClassification() throws Exception {

    }

    /**
     * A homotypical synonym from a source which points to a name which is considered a heterotypical synonym in the nub
     * must be converted into a heterotypical synoym.
     *
     * In the synonymy here: http://cichorieae.e-taxonomy.net/portal/cdm_dataportal/taxon/209399b6-0d3c-4f5a-9f0d-b49ebe0f9403/synonymy
     * the Macrorhynchus humilis group (and all others below) are heterotypical synoyms of Agoseris apargioides, but they have homotypical
     * synonyms listed under them. The final synonym relationship from Agoseris apargioides var. maritima to Agoseris apargioides is therefore
     * a heterotypical one, not homotypic!
     */
    @Test
    @Ignore("write test")
    public void testHomoToHeteroTypicalSynonym() throws Exception {

    }

    /**
     * Virus names are not parsable, do extra test to verify behavior
     */
    @Test
    public void testVirusNames() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(22, 23);
        src.setSourceRank(22, Rank.ORDER);
        src.setSourceRank(23, Rank.GENUS);
        build(src);

        NubUsage u = assertScientific("Ranid herpesvirus 1", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
        assertEquals(2, u.sourceIds.size());

        u = assertScientific("Varicellovirus", Rank.GENUS, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
        assertEquals(2, u.sourceIds.size());

        assertScientific("Cervid herpesvirus 1", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, u);
        assertScientific("Cervid herpesvirus 2", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, u);

        u = assertScientific("Herpesvirales", Rank.ORDER, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
        assertScientific("Phaseolus vulgaris Tpv2-6 virus", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, u);
        assertScientific("Chickpea stunt disease associated virus", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, u);
    }

    /**
     * http://dev.gbif.org/issues/browse/POR-2815
     */
    @Test
    public void testGenusYears() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(35);
        build(src);

        // Heliopyrgus
        NubUsage gen = assertCanonical("Heliopyrgus", Rank.GENUS, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
        assertEquals(0, gen.issues.size());

        NubUsage spec = assertCanonical("Heliopyrgus willi", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, gen);
        assertTrue(spec.issues.contains(NameUsageIssue.PUBLISHED_BEFORE_GENUS));

        NubUsage u = assertCanonical("Heliopyrgus willisyn", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.SYNONYM, spec);
        assertFalse(u.issues.contains(NameUsageIssue.PUBLISHED_BEFORE_GENUS));

        u = assertCanonical("Heliopyrgus willi banane", Rank.SUBSPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, spec);
        assertTrue(u.issues.contains(NameUsageIssue.PUBLISHED_BEFORE_GENUS));

        u = assertCanonical("Heliopyrgus correctwilli", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, gen);
        assertFalse(u.issues.contains(NameUsageIssue.PUBLISHED_BEFORE_GENUS));


        // Meliopyrgus
        gen = assertCanonical("Meliopyrgus", Rank.GENUS, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
        assertEquals(0, gen.issues.size());
        u = assertCanonical("Meliopyrgus willi", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, gen);
        assertFalse(u.issues.contains(NameUsageIssue.PUBLISHED_BEFORE_GENUS));
        u = assertCanonical("Meliopyrgus correctwilli", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, gen);
        assertFalse(u.issues.contains(NameUsageIssue.PUBLISHED_BEFORE_GENUS));


        // Leliopyrgus
        gen = assertCanonical("Leliopyrgus", Rank.GENUS, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
        assertEquals(0, gen.issues.size());
        u = assertCanonical("Leliopyrgus willi", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, gen);
        assertFalse(u.issues.contains(NameUsageIssue.PUBLISHED_BEFORE_GENUS));
        u = assertCanonical("Leliopyrgus correctwilli", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, gen);
        assertFalse(u.issues.contains(NameUsageIssue.PUBLISHED_BEFORE_GENUS));
    }

    /**
     * builds a new nub and keeps dao open for further test queries.
     */
    private void build(NubSourceList src) {
        NubBuilder nb = NubBuilder.create(dao, src, new IdLookupImpl(Lists.<LookupUsage>newArrayList()), 10);
        nb.run();
        IdGenerator.Metrics metrics = nb.idMetrics();
        System.out.println(metrics);

        tx = dao.beginTx();
        dao.printTree();

        // assert we have only ever 8 root taxa - the kingdoms
        assertEquals(Kingdom.values().length, countRoot());

        // assert we have unique ids
        assertUniqueIds();
    }

    private void assertUniqueIds() {
        Set<Integer> keys = Sets.newHashSet();
        for (Node n : IteratorUtil.loop(dao.allTaxa())) {
            NubUsage u = dao.readNub(n);
            if (keys.contains(u.usageKey)) {
                System.err.println(u);
                fail("Nub keys not unique: " + u.usageKey);
            } else {
                keys.add(u.usageKey);
            }
        }
    }

    private void rebuild(NubSourceList src) {
        IdLookupImpl previousIds = new IdLookupImpl(dao);
        tx.close();
        dao.close();
        // new, empty DAO
        dao = UsageDao.temporaryDao(100);
        NubBuilder nb = NubBuilder.create(dao, src, previousIds, previousIds.getKeyMax()+1);
        nb.run();

        IdGenerator.Metrics metrics = nb.idMetrics();
        System.out.println(metrics);

        tx = dao.beginTx();
        dao.printTree();

        // assert we have only ever 8 root taxa - the kingdoms
        assertEquals(Kingdom.values().length, countRoot());
        // assert we have unique ids
        assertUniqueIds();
    }

    private void assertClassification(NubUsage nub, String... parentNames) {
        int idx = 0;
        for (NubUsage n : parents(nub.node)) {
            assertEquals("Higher classification mismatch for " + nub.parsedName.getScientificName(), parentNames[idx++], n.parsedName.canonicalName());
        }
    }

    private NubUsage assertCanonical(String canonical, @Nullable String authorship, @Nullable NamePart notho, Rank rank, Origin origin) {
        return assertCanonical(canonical, authorship, notho, rank, null, origin, null, null);
    }

    private NubUsage assertCanonical(String canonical, Rank rank, Origin origin, @Nullable NubUsage parent) {
        return assertCanonical(canonical, null, null, rank, null, origin, null, parent);
    }

    private NubUsage assertCanonical(String canonical, Rank rank, Origin origin, Kingdom k, @Nullable NubUsage parent) {
        return assertCanonical(canonical, null, null, rank, k, origin, null, parent);
    }

    private NubUsage assertCanonical(String canonical, Rank rank, Origin origin, @Nullable TaxonomicStatus status, @Nullable NubUsage parent) {
        return assertCanonical(canonical, null, null, rank, null, origin, status, parent);
    }

    private NubUsage assertCanonical(String canonical, String authorship, Rank rank, Origin origin) {
        return assertCanonical(canonical, authorship, null, rank, null, origin, null, null);
    }

    private NubUsage assertCanonical(String canonical, @Nullable String authorship, @Nullable NamePart notho, Rank rank, Origin origin, @Nullable TaxonomicStatus status, @Nullable NubUsage parent, NameUsageIssue ... issues) {
        return assertCanonical(canonical, authorship, notho, rank, null, origin, status, parent, issues);
    }

    private NubUsage assertCanonical(String canonical, @Nullable String authorship, @Nullable NamePart notho, Rank rank, Kingdom k, Origin origin, @Nullable TaxonomicStatus status, @Nullable NubUsage parent, NameUsageIssue ... issues) {
        NubUsage u = getCanonical(canonical, rank, k);
        assertNub(u, canonical, authorship, notho, rank, origin, status, parent);
        if (k != null) {
            assertEquals("wrong kingdom " + k, k, u.kingdom);
        }
        assertEquals("wrong canonical name for " + canonical, canonical, UsageDao.canonicalOrScientificName(u.parsedName, false));
        for (NameUsageIssue issue : issues) {
            assertTrue("missing issue "+issue, u.issues.contains(issue));
        }
        return u;
    }

    private NubUsage assertScientific(String sciname, Rank rank, Origin origin, @Nullable TaxonomicStatus status, @Nullable NubUsage parent) {
        NubUsage u = getScientific(sciname, rank);
        assertNub(u, sciname, null, null, rank, origin, status, parent);
        assertEquals("wrong scientific name for " + sciname, sciname, UsageDao.canonicalOrScientificName(u.parsedName, true));
        return u;
    }

    private void assertNotExisting(String sciname, Rank rank) {
        NubUsage u = getScientific(sciname, rank);
        assertNull("name wrongly exists: " + sciname, u);
    }

    private void assertNub(NubUsage u, String name, @Nullable String authorship, @Nullable NamePart notho, Rank rank, Origin origin, @Nullable TaxonomicStatus status, @Nullable NubUsage parent) {
        assertNotNull("Missing " + rank + " " + name, u);
        assertEquals("wrong rank for " + name, rank, u.rank);
        assertEquals("wrong origin for " + name, origin, u.origin);
        if (authorship != null) {
            assertEquals("wrong authorship for " + name, authorship, u.parsedName.authorshipComplete());
        }
        assertEquals("wrong notho for " + name, notho, u.parsedName.getNotho());
        if (status != null) {
            assertEquals("wrong status for " + name, status, u.status);
        }
        if (parent != null) {
            NubUsage p2 = parentOrAccepted(u.node);
            assertEquals("wrong parent "+p2.parsedName.canonicalNameComplete()+" for " + name, p2.node, parent.node);
        }
    }

    private List<NubUsage> children(Node parent) {
        List<NubUsage> usages = Lists.newArrayList();
        for (Node n : Traversals.CHILDREN.traverse(parent).nodes()) {
            usages.add(get(n));
        }
        return usages;
    }

    private List<NubUsage> parents(Node child) {
        List<NubUsage> usages = Lists.newArrayList();
        for (Node n : Traversals.PARENTS.traverse(child).nodes()) {
            usages.add(get(n));
        }
        return usages;
    }

    private NubUsage parentOrAccepted(Node child) {
        Relationship rel = child.getSingleRelationship(RelType.PARENT_OF, Direction.INCOMING);
        if (rel == null) {
            rel = child.getSingleRelationship(RelType.SYNONYM_OF, Direction.OUTGOING);
        }
        return get(rel.getOtherNode(child));
    }

    private List<NubUsage> listCanonical(String canonical) {
        List<NubUsage> usages = Lists.newArrayList();
        for (Node n : IteratorUtil.loop(dao.getNeo().findNodes(Labels.TAXON, NodeProperties.CANONICAL_NAME, canonical))) {
            usages.add(get(n));
        }
        return usages;
    }

    private List<NubUsage> listSynonyms (NubUsage acc) {
        List<NubUsage> usages = Lists.newArrayList();
        for (Relationship rel : acc.node.getRelationships(RelType.SYNONYM_OF, Direction.INCOMING)) {
            Node syn = rel.getOtherNode(acc.node);
            usages.add(get(syn));
        }
        return usages;
    }

    public NameUsage getUsage(Node n) {
        return dao.readUsage(n, true);
    }

    private NameUsage getUsage(int key) {
        return getUsage(dao.getNeo().getNodeById(key));
    }

    private List<NubUsage> listScientific(String sciname) {
        List<NubUsage> usages = Lists.newArrayList();
        for (Node n : IteratorUtil.loop(dao.getNeo().findNodes(Labels.TAXON, NodeProperties.SCIENTIFIC_NAME, sciname))) {
            usages.add(get(n));
        }
        return usages;
    }

    private NubUsage getScientific(String sciname, Rank rank) {
        return getOne(listScientific(sciname), rank, null, sciname);
    }

    private NubUsage getCanonical(String canonical, Rank rank) {
        return getOne(listCanonical(canonical), rank, null, canonical);
    }

    private NubUsage getCanonical(String canonical, Rank rank, Kingdom k) {
        return getOne(listCanonical(canonical), rank, k, canonical);
    }

    private NubUsage getOne(List<NubUsage> usages, Rank rank, @Nullable Kingdom k, String name) {
        Iterator<NubUsage> iter = usages.iterator();
        while (iter.hasNext()) {
            NubUsage u = iter.next();
            if (u.rank != rank) {
                iter.remove();
            } else if (k != null && u.kingdom != k) {
                iter.remove();
            }
        }
        if (usages.isEmpty()) {
            return null;
        } else if (usages.size() == 1) {
            return usages.get(0);
        }
        throw new IllegalStateException("Too many usages for " + rank + " " + name);
    }

    private NubUsage get(String canonical) {
        return get(dao.getNeo().findNode(Labels.TAXON, NodeProperties.CANONICAL_NAME, canonical));
    }

    private NubUsage get(int key) {
        return get(dao.getNeo().getNodeById(key));
    }

    private NubUsage get(Node n) {
        if (n == null) {
            return null;
        }
        NubUsage nub = dao.readNub(n);
        nub.node = n;
        return nub;
    }

    private long countTaxa() {
        return IteratorUtil.count(dao.getNeo().findNodes(Labels.TAXON));
    }

    private long countSpecies() {
        return IteratorUtil.count(dao.getNeo().findNodes(Labels.SPECIES));
    }

    private long countRoot() {
        return IteratorUtil.count(dao.getNeo().findNodes(Labels.ROOT));
    }

    class TreeAsserter implements StartEndHandler {
        private Iterator<NubNode> treeIter;

        public TreeAsserter(NubTree tree) {
            this.treeIter = tree.iterator();
        }

        @Override
        public void start(Node n) {
            String expected = treeIter.next().name;
            String name = (String) n.getProperty(NodeProperties.SCIENTIFIC_NAME);
            assertEquals(expected, name);

            // check for synonyms and sort by name
            for (Node s : TreePrinter.SYNONYM_ORDER.sortedCopy(Traversals.SYNONYMS.traverse(n).nodes())) {
                expected = treeIter.next().name;
                name = (String) s.getProperty(NodeProperties.SCIENTIFIC_NAME);
                assertEquals(expected, name);
            }
        }

        @Override
        public void end(Node n) {
        }

        public boolean completed() {
            return !treeIter.hasNext();
        }
    }

    private void assertTree(String filename) throws IOException {
        NubTree expected = NubTree.read("trees/" + filename);
        assertEquals("Number of roots differ", expected.getRoot().children.size(), IteratorUtil.count(dao.allRootTaxa()));
        TreeAsserter treeAssert = new TreeAsserter(expected);
        TaxonWalker.walkAccepted(dao.getNeo(), null, treeAssert);
        assertTrue("There should be more taxa", treeAssert.completed());
    }
}