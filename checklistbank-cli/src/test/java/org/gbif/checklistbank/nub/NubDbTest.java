package org.gbif.checklistbank.nub;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;

import com.google.common.base.Throwables;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NubDbTest {
    final NameParser parser = new NameParser();
    private long counter = 1;

    @Test
    public void testCountTaxa() throws Exception {
        UsageDao dao = UsageDao.temporaryDao(10);
        NubDb nub = NubDb.create(dao, AuthorComparator.createWithoutAuthormap());
        try (Transaction tx = dao.beginTx()){

            assertEquals(0l, nub.countTaxa());

            NubUsage u = new NubUsage();
            u.parsedName = new ParsedName();
            u.origin = Origin.SOURCE;
            u.rank = Rank.SPECIES;
            nub.addRoot(u);
            assertEquals(1l, nub.countTaxa());

            // we add the same nub usage which already has a neo node, nothing changes
            nub.addRoot(u);
            assertEquals(1l, nub.countTaxa());

            u.node = null;
            nub.addRoot(u);
            assertEquals(2l, nub.countTaxa());
            tx.success();
        }

    }

    @Test
    public void testFindAcceptedCanonical() throws Exception {
        UsageDao dao = UsageDao.temporaryDao(10);
        NubDb db = NubDb.create(dao,  AuthorComparator.createWithoutAuthormap());
        try (Transaction tx = dao.beginTx()){

            final NubUsage plantae = db.addRoot(buildNub(Kingdom.PLANTAE, "Plantae", Rank.KINGDOM, TaxonomicStatus.ACCEPTED));
            final NubUsage oenanteP = db.addUsage(plantae, buildNub("Oenanthe Vieillot, 1816", Rank.GENUS, TaxonomicStatus.ACCEPTED));
            db.addUsage(oenanteP, buildNub("Oenanthe aquatica Poir.", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
            NubUsage acc = db.addUsage(plantae, buildNub("Palma aquatica (Senser.)", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
            db.addUsage(acc, buildNub("Oenanthe aquatica Senser.", Rank.SPECIES, TaxonomicStatus.SYNONYM));
            db.addUsage(oenanteP, buildNub("Oenanthe carolina (Mill.)", Rank.SPECIES, TaxonomicStatus.DOUBTFUL));
            acc = db.addUsage(oenanteP, buildNub("Oenanthe arida", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
            db.addUsage(acc, buildNub("Onatus horrida", Rank.SPECIES, TaxonomicStatus.SYNONYM));
            db.addUsage(acc, buildNub("Onatus horrida alpina", Rank.SUBSPECIES, TaxonomicStatus.SYNONYM));

            final NubUsage animalia = db.addRoot(buildNub(Kingdom.ANIMALIA, "Animalia", Rank.KINGDOM, TaxonomicStatus.ACCEPTED));
            final NubUsage oenanteA = db.addUsage(animalia, buildNub("Oenanthe Linnaeus, 1753", Rank.GENUS, TaxonomicStatus.ACCEPTED));
            db.addUsage(animalia, buildNub("Geotrupes stercorarius (Linnaeus, 1758)", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
            acc = db.addUsage(animalia, buildNub("Geotrupes spiniger (Marsham, 1802)", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
            db.addUsage(acc, buildNub("Geotrupes stercorarius Erichson, 1847", Rank.SPECIES, TaxonomicStatus.SYNONYM));
            db.addUsage(oenanteA, buildNub("Oenanthe aquatica", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
            tx.success();

            assertTrue(db.findAcceptedNubUsage(Kingdom.ANIMALIA, "Oenanthe", Rank.GENUS).isMatch());
            assertTrue(db.findAcceptedNubUsage(Kingdom.ANIMALIA, "Oenanthe aquatica", Rank.SPECIES).isMatch());
            assertFalse(db.findAcceptedNubUsage(Kingdom.ANIMALIA, "Oenanthe aquatica", Rank.SUBSPECIES).isMatch());

            assertFalse(db.findAcceptedNubUsage(Kingdom.PLANTAE, "Onatus horrida alpina", Rank.SUBSPECIES).isMatch());
            assertFalse(db.findAcceptedNubUsage(Kingdom.PLANTAE, "Onatus horrida", Rank.SPECIES).isMatch());
            assertTrue(db.findAcceptedNubUsage(Kingdom.PLANTAE, "Oenanthe", Rank.GENUS).isMatch());
            assertTrue(db.findAcceptedNubUsage(Kingdom.PLANTAE, "Oenanthe aquatica", Rank.SPECIES).isMatch());
            assertFalse(db.findAcceptedNubUsage(Kingdom.PLANTAE, "Oenanthe aquatica", Rank.SUBSPECIES).isMatch());
        }
    }

    @Test
    public void testGetParent() throws Exception {
        UsageDao dao = UsageDao.temporaryDao(10);
        NubDb db = NubDb.create(dao,  AuthorComparator.createWithoutAuthormap());
        try (Transaction tx = dao.beginTx()){
            final NubUsage plantae = db.addRoot(buildNub(Kingdom.PLANTAE, "Plantae", Rank.KINGDOM, TaxonomicStatus.ACCEPTED));
            final NubUsage oenanteP = db.addUsage(plantae, buildNub("Oenanthe Vieillot, 1816", Rank.GENUS, TaxonomicStatus.ACCEPTED));
            db.addUsage(oenanteP, buildNub("Oenanthe aquatica Poir.", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
            NubUsage acc = db.addUsage(plantae, buildNub("Palma aquatica (Senser.)", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
            db.addUsage(acc, buildNub("Oenanthe aquatica Senser.", Rank.SPECIES, TaxonomicStatus.SYNONYM));
            db.addUsage(oenanteP, buildNub("Oenanthe carolina (Mill.)", Rank.SPECIES, TaxonomicStatus.DOUBTFUL));

            assertNull(db.getParent((NubUsage) null));
            assertNull(db.getParent(new NubUsage()));
            assertNull(db.getParent(plantae));
            assertNotNull(db.getParent(oenanteP));
        }
    }

    @Test
    @Ignore("Authorship handling needs to be settled first")
    public void testFindTaxa() throws Exception {
        UsageDao dao = UsageDao.temporaryDao(10);
        NubDb db = NubDb.create(dao,  AuthorComparator.createWithoutAuthormap());
        try (Transaction tx = dao.beginTx()){

        }
    }

    private NubUsage buildNub(String sciname, Rank rank, TaxonomicStatus status) {
        return buildNub(null, sciname, rank, status);
    }
    private NubUsage buildNub(Kingdom k, String sciname, Rank rank, TaxonomicStatus status) {
        NubUsage nu = new NubUsage();
        try {
            nu.parsedName = parser.parse(sciname, rank);
            nu.rank = rank;
            nu.status = status;
            nu.kingdom = k;
        } catch (UnparsableException e) {
            Throwables.propagate(e);
        }
        return nu;
    }

    private SrcUsage buildSrc(String sciname, Rank rank, TaxonomicStatus status) {
        return null;
    }
}