package org.gbif.checklistbank.nub;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.common.NoneMatchingService;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NodeProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.source.ClasspathUsageSource;
import org.gbif.checklistbank.nub.source.UsageSource;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

import com.beust.jcommander.internal.Lists;
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

public class NubBuilderTest {
  private UsageDao dao;
  private Transaction tx;

  @Before
  public void init(){
    dao = UsageDao.temporaryDao(128);
  }

  @After
  public void shutdown(){
    if (tx != null){
      tx.close();
    }
    dao.closeAndDelete();
  }

  @Test
  public void testKingdoms() throws Exception {
    build(ClasspathUsageSource.emptySource());
    // bad ranks, no source usages should be created
    assertEquals(Kingdom.values().length, countTaxa());
  }

  /**
   * The GBIF backbone only accepts mayor linnean ranks above species level.
   * For infraspecific names we only accept subspecies in zoology, but other ranks can treated as synonyms.
   * In botany subspecies, variety or form is an accepted rank.
   */
  @Test
  public void testBackboneRanks() throws Exception {
    build(ClasspathUsageSource.source(1));

    // bad ranks, no source usages should be created
    assertTrue(listCanonical("Lepiota nuda maxima").isEmpty());
    assertTrue(listCanonical("Agaricaceaes").isEmpty());
    assertTrue(listCanonical("Francisella tularensis rosensis").isEmpty());
    assertTrue(listCanonical("Francisella tularensis tularensis").isEmpty());
  }

  @Test
  public void testUnknownKingdom() throws Exception {
    build(ClasspathUsageSource.source(4));

    NubUsage k = assertCanonical(Kingdom.INCERTAE_SEDIS.scientificName(), Rank.KINGDOM, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
    NubUsage f = assertCanonical("Popeliaceae", Rank.FAMILY, Origin.SOURCE, TaxonomicStatus.ACCEPTED, k);
    NubUsage g = assertCanonical("Lepiota", Rank.GENUS, Origin.IMPLICIT_NAME, TaxonomicStatus.ACCEPTED, f);
    NubUsage u = assertCanonical("Lepiota nuda", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, g);
  }

  @Test
  public void testUpdateAuthorship() throws Exception {
    build(ClasspathUsageSource.source(1, 5, 6));

    assertCanonical("Agaricaceae", "Yoda", Rank.FAMILY, Origin.SOURCE);
    assertCanonical("Lepiota seminuda", "Miller", Rank.SPECIES, Origin.SOURCE);
    assertCanonical("Lepiota nuda elegans", "DC.", Rank.SUBSPECIES, Origin.SOURCE);
    assertCanonical("Lepiota nuda nuda", "", Rank.SUBSPECIES, Origin.AUTONYM);
    assertCanonical("Lepiota nuda europaea", "Döring", Rank.VARIETY, Origin.SOURCE);
  }

  @Test
  @Ignore("TODO: NEEDS FIXING")
  public void testUpdateClassification() throws Exception {
    ClasspathUsageSource src = ClasspathUsageSource.source(3, 5, 7);
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
    build(ClasspathUsageSource.source(1));

    NubUsage genus = assertCanonical("Lepiota", Rank.GENUS, Origin.IMPLICIT_NAME, null);

    assertCanonical("Lepiota seminuda", Rank.SPECIES, Origin.SOURCE, genus);

    final NubUsage species = assertCanonical("Lepiota nuda", Rank.SPECIES, Origin.IMPLICIT_NAME, genus);

    assertCanonical("Lepiota nuda elegans", Rank.SUBSPECIES, Origin.SOURCE, species);

    assertCanonical("Lepiota nuda europaea", Rank.VARIETY, Origin.SOURCE, species);
  }

  /**
   * Accepted infraspecific names must have a corresponding autonym in both the zoological and botanical code.
   * In zoology we only accept infraspecies taxa at the rank of subspecies.
   * In botany it can be at subspecies, variety or form level.
   *
   * For synonyms autonyms are NOT generated.
   */
  @Test
  public void testCreateImplicitAutonym() throws Exception {
    build(ClasspathUsageSource.source(1));

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
    ClasspathUsageSource src = ClasspathUsageSource.source(3);
    src.setSourceRank(3, Rank.KINGDOM);
    build(src);

    assertNotNull(getCanonical("Animalia", Rank.KINGDOM));
    assertNotNull(getCanonical("Coleoptera", Rank.ORDER));
    assertNotNull(getCanonical("Poaceae", Rank.FAMILY));
  }


  @Test
  public void testColAdiantumSynonym() throws Exception {
    ClasspathUsageSource src = ClasspathUsageSource.source(8);
    src.setSourceRank(8, Rank.PHYLUM);
    build(src);

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
          fail("Unexpected name "+u.parsedName.getScientificName());
      }
    }
  }

  /**
   * An accepted species with a genus that the nub already considers as a synonym should not be accepted.
   * Try to combine the epithet to the accepted genus and if its a new name make it doubtful until we hit another source with that name.
   */
  @Test
  public void testSpeciesInSynonymGenus() throws Exception {
    ClasspathUsageSource src = ClasspathUsageSource.source(11, 12);
    build(src);

    NubUsage oct = assertCanonical("Octopus", "Cuvier, 1797", null, Rank.GENUS, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
    NubUsage amph = assertCanonical("Amphioctopus", "Fischer, 1882", null, Rank.GENUS, Origin.SOURCE, TaxonomicStatus.SYNONYM, oct);

    assertCanonical("Octopus vulgaris", "Cuvier, 1797", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, oct);
    NubUsage species = assertCanonical("Octopus fangsiao", "d'Orbigny, 1839", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, oct);
    assertCanonical("Amphioctopus fangsiao", "(d'Orbigny, 1835)", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.SYNONYM, species);

    species = assertCanonical("Octopus markus", "(Döring, 1999)", null, Rank.SPECIES, Origin.AUTO_RECOMBINATION, TaxonomicStatus.DOUBTFUL, oct);
    assertCanonical("Amphioctopus markus", "Döring, 1999", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.SYNONYM, species);
  }

  /**
   * The genus name Oenanthe is used as
   * 1. accepted bird genus Oenanthe Vieillot, 1816
   * 2. accepted plant genus Oenanthe Linnaeus, 1753
   *
   *
   * Similar the genus Trichoneura:
   * http://www.catalogueoflife.org/col/search/all/key/Trichoneura/match/1
   *
   *
   *
   * The genus Blattaria exists 4 times in IRMNG:
   * 1. Blattaria P. Miller, 1754  [Scrophulariaceae]
   * 2. Blattaria O. Kuntze, 1891  [Malvaceae] SYN of Pentapetes Linnaeus 1753
   * 3. Blattaria Voet, ?, 1806  [Coleoptera]
   * 4. Blattaria Weyenbergh, 1874  [Orthoptera fossil]
   *
   * Blattaria only exists as synonym species names in CoL.
   * Should there be any accepted genus at all in GBIF?
   */
  @Test
  public void testHomonymGenus() throws Exception {
    ClasspathUsageSource src = ClasspathUsageSource.source(3,2);
    build(src);

    assertEquals(2, listCanonical("Oenanthe").size());

    assertEquals(2, listCanonical("Trichoneura").size());

    assertEquals(4, listCanonical("Blattaria").size());
  }


  @Test
  public void testHybrids() throws Exception {
    ClasspathUsageSource src = ClasspathUsageSource.source(9);
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
    ClasspathUsageSource src = ClasspathUsageSource.source(10);
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
   * CoL contains the genus Albizia twice within the plants as an accepted name (Fabaceae & Asteraceae).
   * http://www.catalogueoflife.org/col/details/species/id/17793647/source/tree
   * http://www.catalogueoflife.org/col/details/species/id/11468181/source/tree
   *
   * The backbone should only contain one accepted genus in Fabaceae.
   * The Asteraceae one as doubtful.
   */
  @Test
  @Ignore("TODO: NEEDS FIXING")
  public void testAlbiziaCoL() throws Exception {
    ClasspathUsageSource src = ClasspathUsageSource.source(13);
    src.setSourceRank(13, Rank.FAMILY);
    build(src);

    NubUsage fab = assertCanonical("Fabaceae", "", null, Rank.FAMILY, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);

    NubUsage genus = assertCanonical("Albizia", "", null, Rank.GENUS, Origin.IMPLICIT_NAME, TaxonomicStatus.ACCEPTED, fab);
    assertCanonical("Albizia tomentosa", "(Micheli) Standl.", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, genus);
    assertCanonical("Albizia adianthifolia", "(Schum.) W.Wight", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, genus);

    genus = assertCanonical("Albi", "", null, Rank.GENUS, Origin.SOURCE, TaxonomicStatus.ACCEPTED, fab);
    assertCanonical("Albi tomentosa", "(Micheli) Standl.", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, genus);
    assertCanonical("Albi adianthifolia", "(Schum.) W.Wight", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, genus);
    assertCanonical("Albi minki", "W. Wight", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, genus);
  }

  /**
   * CoL contains concept synonyms using the "sensu" notation in the ILDIS GSD (maybe elsewhere too).
   * http://dev.gbif.org/issues/browse/POR-389
   *
   * See http://www.catalogueoflife.org/col/details/species/id/11468181/source/tree
   * Albizia adianthifolia (Schum.) W.Wight has the following synonyms:
   *   Albizia gummifera sensu Capuron, p.p.
   *   Albizia gummifera sensu R.O.Williams
   *   Albizia sassa sensu Aubrev.
   *   Mimosa adianthifolia Schum.
   *
   * Albizia sassa is a synonym twice:
   * http://www.catalogueoflife.org/col/search/all/key/Albizia+sassa/match/1
   *
   * Albizia gummifera is both accepted and a synonym when used as a concept:
   * http://www.catalogueoflife.org/col/search/all/key/Albizia+gummifera/match/1
   *   Albizia gummifera (J.F.Gmel.) C.A.Sm. (ACC)
   *   Albizia gummifera sensu Capuron, p.p. (SYN)
   *   Albizia gummifera sensu R.O.Williams (SYN)
   */
  @Test
  @Ignore("write test")
  public void testSecSynonyms() throws Exception {

  }

  /**
   * The same canonical name with a different author can be used as synonyms multiple times.
   * See http://dev.gbif.org/issues/browse/POR-353
   *
   * Geotrupes stercorarius (Linnaeus, 1758)  (ACC)
   * Geotrupes stercorarius Erichson, 1847    (SYN) for Geotrupes spiniger (Marsham, 1802)
   */
  @Test
  public void testSynonymsWithDifferentAuthors() throws Exception {
    ClasspathUsageSource src = ClasspathUsageSource.source(14);
    build(src);

    assertEquals(2, listCanonical("Geotrupes stercorarius").size());
    NubUsage gen = assertCanonical("Geotrupes", Rank.GENUS, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
    assertScientific("Geotrupes stercorarius (Linnaeus, 1758)", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, gen);
    NubUsage acc = assertCanonical("Geotrupes spiniger", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
    assertScientific("Geotrupes stercorarius Erichson, 1847", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.SYNONYM, acc);

    assertEquals(2, listCanonical("Poa pubescens").size());
    gen = assertCanonical("Poa", Rank.GENUS, Origin.IMPLICIT_NAME, TaxonomicStatus.ACCEPTED, null);
    acc = assertScientific("Poa pratensis L.", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, gen);
    assertScientific("Poa pubescens Lej.", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.SYNONYM, acc);

    gen = assertCanonical("Eragrostis", Rank.GENUS, Origin.IMPLICIT_NAME, TaxonomicStatus.ACCEPTED, null);
    acc = assertScientific("Eragrostis pubescens (R.Br.) Steud.", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, gen);
    assertScientific("Poa pubescens R.Br.", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.SYNONYM, acc);
  }

  /**
   * See http://dev.gbif.org/issues/browse/POR-325
   *
   * Nitrospira is an accepted canonical name at various ranks:
   * 1. genus Nitrospira Watson et al., 1986
   * 2. class Nitrospira
   * 3. phylum Nitrospira
   *
   *
   * Vertebrata is both an accepted subphylum and a genus.
   *
   * Lobata is a genus (algae) & an order (animal)
   * http://dev.gbif.org/issues/browse/POR-362
   */
  @Test
  @Ignore("write test")
  public void testInterrankHomonyms() throws Exception {

  }

  /**
   * Accepted species names can't be homonyms within a kingdom.
   * But for the same canonical name can be a synonym various times in a kingdom.
   */
  @Test
  @Ignore("write test")
  public void testHomonymSpecies() throws Exception {

  }

  /**
   * Pro parte synonyms should exist as a single synonym node with multiple synonym relations
   */
  @Test
  public void testProParteSynonym() throws Exception {
    ClasspathUsageSource src = ClasspathUsageSource.source(15, 16);
    build(src);

    NubUsage u = assertCanonical("Poa pubescens", "Lej.", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.PROPARTE_SYNONYM, null);
    assertEquals(3, u.sourceIds.size());
    List<Relationship> rels = IteratorUtil.asList(u.node.getRelationships(RelType.PROPARTE_SYNONYM_OF, Direction.OUTGOING));
    Relationship acc = IteratorUtil.single(u.node.getRelationships(RelType.SYNONYM_OF, Direction.OUTGOING));
    assertEquals(1, rels.size());
    assertNotEquals(rels.get(0).getEndNode(), acc.getEndNode());
  }

  /**
   * Test merging of taxon information from different sources.
   *
   * Only merge taxonomic infos when taxonomic status is the same
   * and in case of synonyms the accepted name is the same.
   *
   * Nomenclatural info (name authorship, publishedIn, nom status?) can be merged from all records.
   *
   * 1. choose the best name with authorship
   * 2. add nom status for synonyms
   * 3. add publishedIn
   */
  @Test
  @Ignore("write test")
  public void testMergingInfos() throws Exception {

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
   * Synonyms must point to an accepted taxon at the same rank.
   * (Also true for infraspecific ranks in CoL ?)
   */
  @Test
  @Ignore("write test")
  public void testSynonymValidity() throws Exception {

  }

  /**
   * builds a new nub and keeps dao open for further test queries.
   */
  private void build(UsageSource src) {
    NubBuilder nb = NubBuilder.create(dao, src, new NoneMatchingService());
    nb.run();

    tx = dao.beginTx();
    // assert we have only ever 8 root taxa - the kingdoms
    assertEquals(Kingdom.values().length, countRoot());
  }

  private void assertClassification(NubUsage nub, String ... parentNames) {
    int idx = 0;
    for (NubUsage n : parents(nub.node)) {
      assertEquals("Higher classification mismatch for " + nub.parsedName.getScientificName(), parentNames[idx++], n.parsedName.canonicalName());
    }
  }

  private NubUsage assertCanonical(String canonical, @Nullable String authorship, @Nullable NamePart notho, Rank rank, Origin origin) {
    return assertCanonical(canonical, null, notho, rank, origin, null, null);
  }

  private NubUsage assertCanonical(String canonical, Rank rank, Origin origin, @Nullable NubUsage parent) {
    return assertCanonical(canonical, null, null, rank, origin, null, parent);
  }

  private NubUsage assertCanonical(String canonical, Rank rank, Origin origin, @Nullable TaxonomicStatus status, @Nullable NubUsage parent) {
    return assertCanonical(canonical, null, null, rank, origin, status, parent);
  }

  private NubUsage assertCanonical(String canonical, String authorship, Rank rank, Origin origin) {
    return assertCanonical(canonical, authorship, null, rank, origin, null, null);
  }

  private NubUsage assertCanonical(String canonical, @Nullable String authorship, @Nullable NamePart notho, Rank rank, Origin origin, @Nullable TaxonomicStatus status, @Nullable NubUsage parent) {
    NubUsage u = getCanonical(canonical, rank);
    assertNub(u, canonical, authorship, notho, rank, origin, status, parent);
    assertEquals("wrong canonical name for " + canonical, canonical, u.parsedName.canonicalName());
    return u;
  }

  private NubUsage assertScientific(String sciname, Rank rank, Origin origin, @Nullable TaxonomicStatus status, @Nullable NubUsage parent) {
    NubUsage u = getScientific(sciname, rank);
    assertNub(u, sciname, null, null, rank, origin, status, parent);
    assertEquals("wrong scientific name for " + sciname, sciname, u.parsedName.canonicalNameComplete());
    return u;
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
      assertEquals("wrong parent for " + name, p2.node, parent.node);
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
    return get( rel.getOtherNode(child) );
  }

  private List<NubUsage> listCanonical(String canonical) {
    List<NubUsage> usages = Lists.newArrayList();
    for (Node n : IteratorUtil.asIterable(dao.getNeo().findNodes(Labels.TAXON, NodeProperties.CANONICAL_NAME, canonical))) {
      usages.add(get(n));
    }
    return usages;
  }

  private List<NubUsage> listScientific(String sciname) {
    List<NubUsage> usages = Lists.newArrayList();
    for (Node n : IteratorUtil.asIterable(dao.getNeo().findNodes(Labels.TAXON, NodeProperties.SCIENTIFIC_NAME, sciname))) {
      usages.add(get(n));
    }
    return usages;
  }

  private NubUsage getScientific(String sciname, Rank rank) {
    return getOne(listScientific(sciname), rank, sciname);
  }

  private NubUsage getCanonical(String canonical, Rank rank) {
    return getOne(listCanonical(canonical), rank, canonical);
  }

  private NubUsage getOne(List<NubUsage> usages, Rank rank, String name) {
    Iterator<NubUsage> iter = usages.iterator();
    while (iter.hasNext()) {
      NubUsage u = iter.next();
      if (u.rank != rank) {
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

  private long countRoot() {
    return IteratorUtil.count(dao.getNeo().findNodes(Labels.ROOT));
  }

}