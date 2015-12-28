package org.gbif.checklistbank.nub;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.NubUsageMatch;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.utils.RankUtils;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;

import java.util.UUID;

import com.google.common.base.Throwables;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NubDbTest {
  final NameParser parser = new NameParser();
  private long counter = 1;

  @Test
  public void testCountTaxa() throws Exception {
    UsageDao dao = UsageDao.temporaryDao(10);
    NubDb nub = NubDb.create(dao, AuthorComparator.createWithoutAuthormap());
    try (Transaction tx = dao.beginTx()) {

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
    NubDb db = NubDb.create(dao, AuthorComparator.createWithoutAuthormap());
    try (Transaction tx = dao.beginTx()) {

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
    NubDb db = NubDb.create(dao, AuthorComparator.createWithoutAuthormap());
    try (Transaction tx = dao.beginTx()) {
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
  public void testFindTaxa() throws Exception {
    UsageDao dao = UsageDao.temporaryDao(25);
    NubDb db = NubDb.create(dao, AuthorComparator.createWithoutAuthormap());
    try (Transaction tx = dao.beginTx()) {

      final NubUsage plantae = db.addRoot(buildNub(Kingdom.PLANTAE, "Plantae", Rank.KINGDOM, TaxonomicStatus.ACCEPTED));
      final NubUsage animalia = db.addRoot(buildNub(Kingdom.ANIMALIA, "Animalia", Rank.KINGDOM, TaxonomicStatus.ACCEPTED));
      final NubUsage fungi = db.addRoot(buildNub(Kingdom.FUNGI, "Fungi", Rank.KINGDOM, TaxonomicStatus.ACCEPTED));

      final NubUsage sagartiidae = addHierarchy(db, animalia, Rank.PHYLUM, "Cnidaria", "Anthozoa", "Actiniaria", "Sagartiidae");
      final NubUsage verrillactis = db.addUsage(sagartiidae, buildNub("Verrillactis", Rank.GENUS, TaxonomicStatus.ACCEPTED));
      final NubUsage aarum = db.addUsage(sagartiidae, buildNub("Aarum", Rank.GENUS, TaxonomicStatus.ACCEPTED));
      final NubUsage barum = db.addUsage(sagartiidae, buildNub("Barum", Rank.GENUS, TaxonomicStatus.ACCEPTED));
      final NubUsage carum = db.addUsage(sagartiidae, buildNub("Carum", Rank.GENUS, TaxonomicStatus.ACCEPTED));
      final NubUsage darum = db.addUsage(sagartiidae, buildNub("Darum", Rank.GENUS, TaxonomicStatus.ACCEPTED));

      final NubUsage vp1870 = db.addUsage(verrillactis, buildNub("Verrillactis paguri (Verrill, 1870)", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
      final NubUsage vp = db.addUsage(vp1870, buildNub("Verrillactis paguri (Verrill)", Rank.SPECIES, TaxonomicStatus.SYNONYM));
      final NubUsage vp1869 = db.addUsage(vp1870, buildNub("Verrillactis paguri (Verrill, 1869)", Rank.SPECIES, TaxonomicStatus.HETEROTYPIC_SYNONYM));
      final NubUsage vp1896 = db.addUsage(vp1870, buildNub("Verrillactis paguri (Verrill, 1896*a*)", Rank.SPECIES, TaxonomicStatus.SYNONYM));

      final NubUsage vacc = db.addUsage(verrillactis, buildNub("Verrillactis accepturi (Verrill, 1870)", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
      final NubUsage vv = db.addUsage(vacc, buildNub("Verrillactis vagguri (Verrill)", Rank.SPECIES, TaxonomicStatus.SYNONYM));
      db.addUsage(vacc, buildNub("Verrillactis vagguri (Verrill, 1869)", Rank.SPECIES, TaxonomicStatus.HETEROTYPIC_SYNONYM));
      db.addUsage(vacc, buildNub("Verrillactis vagguri (Verrill, 1896*a*)", Rank.SPECIES, TaxonomicStatus.SYNONYM));

      final NubUsage vacc2 = db.addUsage(verrillactis, buildNub("Verrillactis accepturissimo (Verrill, 1870)", Rank.SPECIES, TaxonomicStatus.ACCEPTED));
      db.addUsage(vacc, buildNub("Verrillactis laguri (Verrill)", Rank.SPECIES, TaxonomicStatus.SYNONYM));
      final NubUsage vl = db.addUsage(vacc2, buildNub("Verrillactis laguri (Verrill, 1869)", Rank.SPECIES, TaxonomicStatus.HETEROTYPIC_SYNONYM));
      db.addUsage(vacc2, buildNub("Verrillactis laguri (Verrill, 1896*a*)", Rank.SPECIES, TaxonomicStatus.SYNONYM));
      tx.success();


      // full name matches
      assertMatch(vp1869,
          db.findNubUsage(Constants.COL_DATASET_KEY, buildSrc("Verrillactis paguri (Verrill, 1869)", Rank.SPECIES, TaxonomicStatus.ACCEPTED), Kingdom.ANIMALIA, animalia)
      );
      assertMatch(vv,
          db.findNubUsage(Constants.COL_DATASET_KEY, buildSrc("Verrillactis vagguri (Verrill)", Rank.SPECIES, TaxonomicStatus.ACCEPTED), Kingdom.ANIMALIA, animalia)
      );
      assertMatch(vl,
          db.findNubUsage(Constants.COL_DATASET_KEY, buildSrc("Verrillactis laguri (Verrill, 1869)", Rank.SPECIES, TaxonomicStatus.ACCEPTED), Kingdom.ANIMALIA, animalia)
      );

      // wrong kingdom
      assertNoMatch(
          db.findNubUsage(Constants.COL_DATASET_KEY, buildSrc("Verrillactis paguri (Verrill, 1869)", Rank.SPECIES, TaxonomicStatus.ACCEPTED), Kingdom.FUNGI, animalia)
      );

      // wrong rank
      assertNoMatch(
          db.findNubUsage(Constants.COL_DATASET_KEY, buildSrc("Verrillactis paguri (Verrill, 1869)", Rank.GENUS, TaxonomicStatus.ACCEPTED), Kingdom.ANIMALIA, animalia)
      );

      // single accepted
      assertSnap(vp1870,
          db.findNubUsage(Constants.COL_DATASET_KEY, buildSrc("Verrillactis paguri", Rank.SPECIES, TaxonomicStatus.SYNONYM), Kingdom.ANIMALIA, animalia)
      );

      // 3 synonyms, all pointing to the same accepted. Pick first
      assertSnap(vv,
          db.findNubUsage(Constants.COL_DATASET_KEY, buildSrc("Verrillactis vagguri", Rank.SPECIES, TaxonomicStatus.SYNONYM), Kingdom.ANIMALIA, animalia)
      );

      // same usage source as the synonyms and authorless accepted
      assertNoMatch(
          db.findNubUsage(Constants.COL_DATASET_KEY, buildSrc("Verrillactis vagguri", Rank.SPECIES, TaxonomicStatus.ACCEPTED), Kingdom.ANIMALIA, animalia)
      );

      // different usage source than the synonyms, pick first
      assertSnap(vv,
          db.findNubUsage(Constants.NUB_NETWORK_KEY, buildSrc("Verrillactis vagguri", Rank.SPECIES, TaxonomicStatus.ACCEPTED), Kingdom.ANIMALIA, animalia)
      );

      // 2 different accepted parents for these synonyms, ignore
      assertIgnoreSource(db, Constants.NUB_NETWORK_KEY, buildSrc("Verrillactis laguri", Rank.SPECIES, TaxonomicStatus.ACCEPTED), Kingdom.ANIMALIA, animalia);
    }
  }

  private void assertMatch(NubUsage expected, NubUsageMatch m) {
    assertEquals(expected.node, m.usage.node);
    assertFalse(m.ignore);
  }

  private void assertSnap(NubUsage expected, NubUsageMatch m) {
    assertEquals(expected.node, m.usage.node);
    assertTrue(m.ignore);
  }

  private void assertNoMatch(NubUsageMatch m) {
    assertNull(m.usage);
  }

  private void assertIgnoreSource(NubDb db, UUID currSource, SrcUsage u, Kingdom uKingdom, NubUsage currNubParent) {
    try {
      db.findNubUsage(currSource, u, uKingdom, currNubParent);
      fail("IgnoreSourceUsageException  expected");
    } catch (IgnoreSourceUsageException e) {
      return;
    }
  }

  private NubUsage addHierarchy(NubDb db, NubUsage parent, Rank startRank, String... taxa) {
    Rank rank = startRank;
    for (String name : taxa) {
      parent = db.addUsage(parent, buildNub(name, rank, TaxonomicStatus.ACCEPTED));
      rank = RankUtils.nextLowerLinneanRank(rank);
    }
    return parent;
  }

  private NubUsage buildNub(String sciname, Rank rank, TaxonomicStatus status) {
    return buildNub(null, sciname, rank, status);
  }

  private NubUsage buildNub(Kingdom k, String sciname, Rank rank, TaxonomicStatus status) {
    NubUsage nu = new NubUsage();
    nu.datasetKey = Constants.COL_DATASET_KEY;
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
    SrcUsage u = new SrcUsage();
    try {
      u.scientificName = sciname;
      u.parsedName = parser.parse(sciname, rank);
      u.rank = rank;
      u.status = status;
    } catch (UnparsableException e) {
      Throwables.propagate(e);
    }
    return u;
  }
}