package org.gbif.checklistbank.nub;

import org.gbif.api.model.Constants;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.TaxonProperties;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.source.ClasspathUsageSource;
import org.gbif.checklistbank.nub.source.UsageSource;

import java.util.Iterator;
import java.util.List;

import com.beust.jcommander.internal.Lists;
import org.junit.After;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NubBuilderTest {
  private GraphDatabaseService db;
  private NeoMapper mapper = NeoMapper.instance();
  private Transaction tx;

  @After
  public void shutdown() {
    if (tx != null) {
      tx.close();
    }
    if (db != null) {
      db.shutdown();
    }
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
    assertTrue(list("Lepiota nuda maxima").isEmpty());
    assertTrue(list("Agaricaceaes").isEmpty());
    assertTrue(list("Francisella tularensis rosensis").isEmpty());
    assertTrue(list("Francisella tularensis tularensis").isEmpty());
  }

  @Test
  public void testCreateImplicitGenus() throws Exception {
    build(ClasspathUsageSource.source(1));

    NubUsage u = assertNub("Lepiota", Rank.GENUS, Origin.IMPLICIT_NAME, null);
    final int genusKey = u.getKey();

    assertNub("Lepiota seminuda", Rank.SPECIES, Origin.SOURCE, genusKey);

    u = assertNub("Lepiota nuda", Rank.SPECIES, Origin.IMPLICIT_NAME, genusKey);
    final int speciesKey = u.getKey();

    assertNub("Lepiota nuda elegans", Rank.SUBSPECIES, Origin.SOURCE, speciesKey);

    assertNub("Lepiota nuda europaea", Rank.VARIETY, Origin.SOURCE, speciesKey);
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

    List<NubUsage> nudas = list("Lepiota nuda nuda");
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
    assertTrue(list("Lepiota nuda maxima").isEmpty());
    assertNull(get("Lepiota nuda nuda", Rank.SUBVARIETY));
  }

  @Test
  public void testHigherClassification() throws Exception {
    ClasspathUsageSource src = ClasspathUsageSource.source(3);
    src.setSourceRank(3, Rank.KINGDOM);
    build(src);

    assertNotNull(get("Animalia", Rank.KINGDOM));
    assertNotNull(get("Coleoptera", Rank.ORDER));
    assertNotNull(get("Poaceae", Rank.FAMILY));
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

    assertEquals(2, list("Oenanthe").size());

    assertEquals(2, list("Trichoneura").size());

    assertEquals(4, list("Blattaria").size());
  }

  /**
   * CoL contains the genus Albizia twice within the plant genus as an accepted name (Fabaceae & Asteraceae).
   * http://www.catalogueoflife.org/col/details/species/id/17793647/source/tree
   * http://www.catalogueoflife.org/col/details/species/id/11468181/source/tree
   *
   * The backbone should only contain one accepted genus in Fabaceae.
   * The Asteraceae one as doubtful.
   */
  @Test
  public void testAlbiziaCoL() throws Exception {

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
  public void testInterrankHomonyms() throws Exception {

  }

  /**
   * Accepted species names can't be homonyms within a kingdom.
   * But for the same canonical name can be a synonym various times in a kingdom.
   */
  @Test
  public void testHomonymSpecies() throws Exception {

  }

  /**
   * Pro parte synonyms should exist as a single synonym node with multiple synonym relations
   */
  @Test
  public void testProParteSynonym() throws Exception {

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
  public void testMergingInfos() throws Exception {

  }

  /**
   * Test merging of taxon classification from different sources.
   * Only merge the classification when taxonomic status is the same.
   * For synonyms use the accepted name from later sources in case the primary one is incertae-sedis.
   */
  @Test
  public void testMergingClassification() throws Exception {

  }


  /**
   * Synonyms must point to an accepted taxon at the same rank.
   * (Also true for infraspecific ranks in CoL ?)
   */
  @Test
  public void testSynonymValidity() throws Exception {

  }



  private void build(UsageSource src) {
    NubConfiguration cfg = new NubConfiguration();
    NubBuilder nb = NubBuilder.create(cfg, src);
    nb.run();
    db = cfg.neo.newEmbeddedDb(Constants.NUB_DATASET_KEY, false);
    tx = db.beginTx();

    // assert we have only ever 8 root taxa - the kingdoms
    assertEquals(Kingdom.values().length, countRoot());
  }

  private NubUsage assertNub(String canonical, Rank rank, Origin origin, Integer parentKey) {
    NubUsage u = get(canonical);
    assertEquals(canonical, u.parsedName.canonicalName());
    assertEquals(rank, u.rank);
    assertEquals(origin, u.origin);
    if (parentKey != null) {
      //assertEquals(parentKey, u.getParentKey());
    }
    return u;
  }

  private List<NubUsage> list(String canonical) {
    List<NubUsage> usages = Lists.newArrayList();
    for (Node n : IteratorUtil.asIterable(db.findNodes(Labels.TAXON, TaxonProperties.CANONICAL_NAME, canonical))) {
      usages.add(mapper.read(n, new NubUsage()));
    }
    return usages;
  }

  private NubUsage get(String canonical, Rank rank) {
    List<NubUsage> usages = list(canonical);
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
    throw new IllegalStateException("Too many usages for " + rank + " " + canonical);
  }

  private NubUsage get(String canonical) {
    return get(db.findNode(Labels.TAXON, TaxonProperties.CANONICAL_NAME, canonical));
  }

  private NubUsage get(int key) {
    return get(db.getNodeById(key));
  }

  private NubUsage get(Node n) {
    if (n == null) {
      return null;
    }
    NubUsage nub = mapper.read(n, new NubUsage());
    nub.node = n;
    return nub;
  }

  private long countTaxa() {
    return IteratorUtil.count(db.findNodes(Labels.TAXON));
  }

  private long countRoot() {
    return IteratorUtil.count(db.findNodes(Labels.ROOT));
  }

}