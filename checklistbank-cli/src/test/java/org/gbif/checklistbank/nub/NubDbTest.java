package org.gbif.checklistbank.nub;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;

import com.google.common.base.Throwables;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NubDbTest {
    final NameParser parser = new NameParser();
    private long counter = 1;

    @Test
    public void testCountTaxa() throws Exception {
        UsageDao dao = UsageDao.temporaryDao(10);
        NubDb nub = NubDb.create(dao, 10);

        assertEquals(0l, nub.countTaxa());

        NubUsage u = new NubUsage();
        u.parsedName = new ParsedName();
        u.origin = Origin.SOURCE;
        nub.addRoot(u);
        nub.addRoot(u);

        assertEquals(2l, nub.countTaxa());
    }

    @Test
    @Ignore("Authorship handling needs to be settled first")
    public void testFindTaxa() throws Exception {
        UsageDao dao = UsageDao.temporaryDao(10);
        NubDb db = NubDb.create(dao, 10);

        final NubUsage animalia = db.addRoot(buildNub("Animalia", Rank.KINGDOM, TaxonomicStatus.ACCEPTED));
        final NubUsage plantae = db.addRoot(buildNub("Plantae", Rank.KINGDOM, TaxonomicStatus.ACCEPTED));
        NubUsage oenanteP = db.addUsage(animalia, buildNub("Oenanthe Vieillot, 1816", Rank.GENUS, TaxonomicStatus.ACCEPTED));
        NubUsage oenanteA = db.addUsage(plantae, buildNub("Oenanthe Linnaeus, 1753", Rank.GENUS, TaxonomicStatus.ACCEPTED));
        db.addUsage(oenanteP, buildNub("Oenanthe aquatica Poir.", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
        db.addUsage(oenanteP, buildNub("Oenanthe aquatica Senser. 1957", Rank.SPECIES, TaxonomicStatus.ACCEPTED));

        db.addUsage(animalia, buildNub("Geotrupes stercorarius (Linnaeus, 1758)", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
        NubUsage acc = db.addUsage(animalia, buildNub("Geotrupes spiniger (Marsham, 1802)", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
        db.addUsage(acc, buildNub("Geotrupes stercorarius Erichson, 1847", Rank.SPECIES, TaxonomicStatus.SYNONYM));

        acc = db.addUsage(oenanteP, buildNub("Oenanthe arida", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
        db.addUsage(acc, buildNub("Onatus horrida", Rank.SPECIES, TaxonomicStatus.SYNONYM));
        db.addUsage(acc, buildNub("Onatus horrida alpina", Rank.INFRASPECIFIC_NAME, TaxonomicStatus.SYNONYM));

        assertNotNull(db.findNubUsage("Onatus horrida alpina", Rank.SUBSPECIES));
    }

    private NubUsage buildNub(String sciname, Rank rank, TaxonomicStatus status) {
        NubUsage nu = new NubUsage();
        try {
            nu.parsedName = parser.parse(sciname, rank);
            nu.rank = rank;
            nu.status = status;
        } catch (UnparsableException e) {
            Throwables.propagate(e);
        }
        return nu;
    }

    private SrcUsage buildSrc(String sciname, Rank rank, TaxonomicStatus status) {
        return null;
    }
}