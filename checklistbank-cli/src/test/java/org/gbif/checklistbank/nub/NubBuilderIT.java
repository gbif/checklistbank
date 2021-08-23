package org.gbif.checklistbank.nub;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import org.apache.commons.io.FileUtils;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.vocabulary.*;
import org.gbif.checklistbank.cli.model.GraphFormat;
import org.gbif.checklistbank.cli.model.RankedName;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.iterable.StreamUtils;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.StartEndHandler;
import org.gbif.checklistbank.neo.traverse.Traversals;
import org.gbif.checklistbank.neo.traverse.TreeWalker;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.nub.source.*;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.gbif.checklistbank.utils.SciNameNormalizer;
import org.gbif.nameparser.NameParserGbifV1;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.nub.lookup.straight.LookupUsage;
import org.gbif.utils.ObjectUtils;
import org.junit.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.helpers.collection.Iterators;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class NubBuilderIT {
  public NubConfiguration cfg = new NubConfiguration();
  private UsageDao dao;
  private Transaction tx;
  private static final NameParser PARSER = new NameParserGbifV1();

  @ClassRule
  public static NeoTmpRepoRule neoRepo = new NeoTmpRepoRule();

  public static NubConfiguration defaultConfig() {
    NubConfiguration cfg = new NubConfiguration();
    cfg.groupBasionyms = true;
    cfg.validate = true;
    cfg.runAssertions = false;
    cfg.autoImport = false;
    cfg.neo = neoRepo.cfg;
    cfg.neo.batchSize = 5000;
    cfg.parserTimeout = 250;
    cfg.blacklist = Sets.newHashSet(
        "Unaccepted",
        "Calendrella cinerea ongumaensis",  // https://github.com/gbif/checklistbank/issues/47
        "Tricholaema leucomelan namaqua"  // https://github.com/gbif/checklistbank/issues/46
    );
    cfg.homonymExclusions = new HashMap<>();
    cfg.homonymExclusions.put("Setabis", Lists.newArrayList("Riodinidae"));
    return cfg;
  }

  private static void log(String msg, Object... args) {
    System.out.println(String.format(msg, args));
  }

  @Before
  public void init() {
    // add shell port (standard is 1337, but already taken on OSX) to open the neo4j shell server for debugging!!!
    dao = UsageDao.temporaryDao(128);
    cfg = defaultConfig();
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

  @Test(expected = RuntimeException.class)
  public void testSourceException() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.emptySource();
    src.submitSources(Lists.<ErrorSource>newArrayList(new ErrorSource()));
    build(src);
  }

  public static class ErrorSource extends NubSource {
    public ErrorSource() {
      super(UUID.randomUUID(), "Error Dataset", null, true, null);
    }

    @Override
    protected void initNeo(NeoUsageWriter writer) throws Exception {
      throw new ParseException("I am the chaos monkey", 0);
    }
  }

  /**
   * The GBIF backbone only accepts mayor linnean ranks above species level.
   * For infraspecific names we only accept subspecies in zoology, but other ranks can treated as synonyms.
   * In botany subspecies, variety or form is an accepted rank.
   */
  @Test
  public void testBackboneRanks() throws Exception {
    build(ClasspathSourceList.source(neoRepo.cfg, 1));

    // bad ranks, no source usages should be created
    assertTrue(listCanonical("Lepiota nuda maxima").isEmpty());
    assertTrue(listCanonical("Agaricaceaes").isEmpty());
    assertTrue(listCanonical("Francisella tularensis rosensis").isEmpty());
    assertTrue(listCanonical("Francisella tularensis tularensis").isEmpty());
  }

  /**
   * https://github.com/gbif/checklistbank/issues/165
   */
  @Test
  @Ignore("Manual test to debug memory usage")
  public void debugNubMemoryUsage() throws Exception {
    RandomSourceList sources = RandomSourceList.source(3, neoRepo.cfg, 10, 10000);
    // do a real build
    build(sources);
  }

  /**
   * https://github.com/gbif/checklistbank/issues/165
   */
  @Test
  @Ignore("Manual test to debug memory usage of the nub source only")
  public void debugSourcesMemory() throws Exception {
    RandomSourceList sources = RandomSourceList.source(2, neoRepo.cfg, 10, 10000);
    // just iterate over sources and close them just as the builder does, but do not use them for anything
    for (NubSource src : sources) {
      try (CloseableIterator<SrcUsage> iter = src.iterator()) {
        for (SrcUsage u : Iterators.loop(iter)) {
          System.out.println(u.key + " -> " + u.scientificName);
        }
      } finally {
        src.close();
      }
    }
  }

  /**
   * Test sources directly without the async loader
   * https://github.com/gbif/checklistbank/issues/165
   */
  @Test
  @Ignore("Manual test to debug memory usage of the nub source only")
  public void debugSourcesMemory2() throws Exception {
    final Random rnd = new Random();
    for (int num = 1; num<10000; num++) {
      Kingdom k = Kingdom.byNubUsageKey(rnd.nextInt(9));
      RandomSource src = new RandomSource(10, k, neoRepo.cfg);
      src.init(true, false);
      try (CloseableIterator<SrcUsage> iter = src.iterator()) {
        for (SrcUsage u : Iterators.loop(iter)) {
          System.out.println(u.key + " -> " + u.scientificName);
        }
      } finally {
        src.close();
      }
    }
  }

  @Test
  public void testUnknownKingdom() throws Exception {
    build(ClasspathSourceList.source(neoRepo.cfg, 4));

    NubUsage k = assertCanonical(Kingdom.INCERTAE_SEDIS.scientificName(), Rank.KINGDOM, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, null);
    NubUsage f = assertCanonical("Popeliaceae", Rank.FAMILY, Origin.SOURCE, TaxonomicStatus.ACCEPTED, k);
    NubUsage g = assertCanonical("Lepiota", Rank.GENUS, Origin.IMPLICIT_NAME, TaxonomicStatus.ACCEPTED, f);
    NubUsage u = assertCanonical("Lepiota nuda", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, g);
  }

  @Test
  public void testUpdateAuthorship() throws Exception {
    build(ClasspathSourceList.source(neoRepo.cfg, 1, 5, 6));

    assertCanonical("Lepiota seminuda", "Miller", Rank.SPECIES, Origin.SOURCE);
    assertCanonical("Lepiota nuda elegans", "DC.", Rank.SUBSPECIES, Origin.SOURCE);
    assertCanonical("Lepiota nuda nuda", "", Rank.SUBSPECIES, Origin.AUTONYM);
    assertCanonical("Lepiota nuda europaea", "Döring", Rank.VARIETY, Origin.SOURCE);
    // families dont use authors!
    // http://dev.gbif.org/issues/browse/POR-2877
    assertCanonical("Agaricaceae", "", Rank.FAMILY, Origin.SOURCE);

    assertTree("1 5 6.txt");
  }

  /**
   * Worms declares the same species name twice in different subgenera.
   * One being a synonym of the other. Make sure we only see the accepted species and ignore the synonym which has the exact same name!
   */
  @Test
  public void testWormsSubgenera() throws Exception {
    build(ClasspathSourceList.source(neoRepo.cfg, 43));

    NubUsage genus = assertCanonical("Hyalonema", "Gray, 1832", Rank.GENUS, Origin.SOURCE);
    assertCanonical("Hyalonema grandancora", "Lendenfeld, 1915", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, genus);

    assertTree("43.txt");
  }

  /**
   * Test using real backbone names to verify synonymization of large families.
   */
  @Test
  @Ignore("manual verification only")
  public void testFullFamilies() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 91);
    src.setSourceRank(91, Rank.CLASS);
    build(src, new File("/Users/markus/nub-synonyms.txt"));
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-398
   */
  @Test
  public void testMergeBasionymGroup() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 25, 26);
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

    assertTree("25 26.txt");
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2812
   * http://dev.gbif.org/issues/browse/POR-3140
   */
  @Test
  public void testOrthographicVariants() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 92, 93);
    build(src);

    assertTree("92 93.txt");
  }


  /**
   * http://dev.gbif.org/issues/browse/POR-3106
   */
  @Test
  public void testMissingRecombAuthors() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 94);
    build(src);

    assertTree("94.txt");
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-3065
   */
  @Test
  public void testSynonymizedAutonyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 95);
    build(src);

    assertTree("95.txt");
  }

  @Test
  public void testConflictingBasionyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 95, 97);
    build(src);

    assertTree("95 97.txt");
  }

  @Test
  public void testConflictingBasionymsFlipped() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 96, 97);
    build(src);

    assertTree("96 97.txt");
  }

  @Test
  public void testConflictingBasionymsOrder() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 96);
    build(src);

    assertTree("96.txt");
  }

  @Test
  public void testPreferAcceptedQualifiedName() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 99);
    build(src);

    assertTree("99.txt");
  }

  @Test
  public void testNamesWithSubgenus() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 100);
    build(src);

    assertTree("100.txt");
  }

  /**
   * See http://dev.gbif.org/issues/browse/POR-3147
   * http://www.iapt-taxon.org/nomen/main.php?page=art46
   */
  @Test
  public void testExAuthorSynonyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 101);
    build(src);

    assertTree("101.txt");
  }

  @Test
  public void testAutonymHomonym() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 102, 103);
    build(src);

    assertTree("102 103.txt");
  }

  /**
   * When list of subspecific names without parents are added we immediately create an implicit name for the species and genus.
   * When we subsequently then encounter the species name we need to make sure we select the right one if there are multiple.
   * At least properly accepted ones should be preferred over doubtful ones.
   */
  @Test
  public void testImplicitNameHomonyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 98);
    build(src);

    assertTree("98.txt");
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-284
   * 4 name pairs each with a diacretic version should result in just 4 distinct nub names.
   */
  @Test
  public void testDiacriticNames() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 27);
    build(src);

    assertEquals(2, countSpecies());
    assertEquals(4, countGenera());
  }

  /**
   * Make sure explicit basionym i.e. original name usage relations make it into the backbone.
   * Dataset 21 contains a conflicting basionym for Martes martes, make sure we use the preferred source dataset 20.
   */
  @Test
  public void testExplicitBasionyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 20, 21);
    build(src);

    assertEquals(1, Iterables.count(getCanonical("Mustela martes", Rank.SPECIES).node.getRelationships(RelType.BASIONYM_OF)));
    assertEquals(1, Iterables.count(getCanonical("Martes martes", Rank.SPECIES).node.getRelationships(RelType.BASIONYM_OF)));

    NameUsage u = getUsage(getCanonical("Martes martes", Rank.SPECIES).node);
    assertEquals("Mustela martes Linnaeus, 1758", u.getBasionym());

    u = getUsage(getCanonical("Martes markusis", Rank.SPECIES).node);
    assertEquals("Cellophania markusa Döring, 2001", u.getBasionym());

    u = getUsage(getCanonical("Cellophania markusa", Rank.SPECIES).node);
    assertNull(u.getBasionym());
  }

  /**
   * Verifies that the backbone patch file in github is in good shape!
   * https://github.com/gbif/backbone-patch
   * <p>
   * This also check the presence of 2 Sagittariidae families,
   * see https://github.com/gbif/checklistbank/issues/123
   * <p>
   * WARNING! requires online access and working github !!!
   */
  @Test
  public void testBackbonePatch() throws Exception {
    List<NubSource> ns = new ArrayList<>();

    NubSource patch = new DwcaSource("backbone patch", DwcaSourceTest.BACKBONE_PATCH_DWCA, cfg.neo);
    patch.ignoreRanksAbove = Rank.PHYLUM;
    patch.supragenericHomonymSource = true;
    ns.add(patch);

    NubSource col = new ClasspathSource(146, neoRepo.cfg); // COL hierarchy for 2x Sagittariidae
    col.ignoreRanksAbove = Rank.KINGDOM;
    col.supragenericHomonymSource = true;
    ns.add(col);

    NubSourceList srcList = new NubSourceList(new NubConfiguration());
    srcList.submitSources(ns);
    build(srcList);

    // do not assert tree as the patch file changes all the time
    // make sure we only get 2x Sagittariidae
    assertEquals(2, listCanonical("Sagittariidae").size());

    // 1 Ambrosia only, see https://github.com/gbif/checklistbank/issues/143
    assertEquals(1, listCanonical("Ambrosia").size());

    // 2x Personidae https://github.com/gbif/portal-feedback/issues/3170
    assertEquals(2, listCanonical("Personidae").size());
  }

  @Test
  @Ignore("for manual debugging only. Depends on external resources")
  public void testExternal() throws Exception {
    List<NubSource> ns = new ArrayList<>();

    NubSource patch = new DwcaSource("Official Lists and Indexes of Names in Zoology",
        new URL("https://github.com/gbif/iczn-lists/archive/master.zip"), cfg.neo
    );
    //patch.ignoreRanksAbove = Rank.PHYLUM;
    //patch.supragenericHomonymSource = true;
    ns.add(patch);

    NubSourceList srcList = new NubSourceList(new NubConfiguration());
    srcList.submitSources(ns);
    build(srcList);
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2786
   */
  @Test
  public void testStableIds() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 3, 2);
    src.setSourceRank(3, Rank.KINGDOM);
    build(src);

    NubUsage u = getScientific("Oenanthe Vieillot, 1816", Rank.GENUS);
    assertEquals("Oenanthe Vieillot, 1816", u.parsedName.canonicalNameComplete());
    assertEquals("Oenanthe Vieillot, 1816", u.parsedName.getScientificName());
    int o1 = u.usageKey;

    u = getScientific("Oenanthe Linnaeus, 1753", Rank.GENUS);
    assertEquals("Oenanthe Linnaeus, 1753", u.parsedName.canonicalNameComplete());
    assertEquals("Oenanthe Linnaeus, 1753", u.parsedName.getScientificName());
    int o2 = u.usageKey;

    int t1 = getScientific("Trichoneura bontocensis Alexander, 1934", Rank.SPECIES).usageKey;
    int t2 = getScientific("Trichoneura hirtella Napper", Rank.SPECIES).usageKey;
    int t1p = parentOrAccepted(getScientific("Trichoneura bontocensis Alexander, 1934", Rank.SPECIES).node).usageKey;
    int t2p = parentOrAccepted(getScientific("Trichoneura hirtella Napper", Rank.SPECIES).node).usageKey;

    int b1 = getScientific("Blattaria P.Miller, 1754", Rank.GENUS).usageKey;
    int b2 = getScientific("Blattaria O.Kuntze, 1891", Rank.GENUS).usageKey;
    int b3 = getScientific("Blattaria Voet, 1806", Rank.GENUS).usageKey;
    int b4 = getScientific("Blattaria Weyenbergh, 1874", Rank.GENUS).usageKey;

    // rebuild nub with additional sources!
    src = ClasspathSourceList.source(neoRepo.cfg, 3, 2, 8, 11);
    src.setSourceRank(3, Rank.KINGDOM);
    rebuild(src);

    assertTree("3 2 8 11.txt");

    // assert ids havent changed!
    u = getScientific("Oenanthe Vieillot, 1816", Rank.GENUS);
    assertEquals("Oenanthe Vieillot, 1816", u.parsedName.canonicalNameComplete());
    assertEquals("Oenanthe Vieillot, 1816", u.parsedName.getScientificName());
    assertEquals(o1, u.usageKey);

    u = getScientific("Oenanthe Linnaeus, 1753", Rank.GENUS);
    assertEquals("Oenanthe Linnaeus, 1753", u.parsedName.canonicalNameComplete());
    assertEquals("Oenanthe Linnaeus, 1753", u.parsedName.getScientificName());
    assertEquals(o2, u.usageKey);

    assertEquals(t1, getScientific("Trichoneura bontocensis Alexander, 1934", Rank.SPECIES).usageKey);
    assertEquals(t2, getScientific("Trichoneura hirtella Napper", Rank.SPECIES).usageKey);
    assertEquals(t1p, parentOrAccepted(getScientific("Trichoneura bontocensis Alexander, 1934", Rank.SPECIES).node).usageKey);
    assertEquals(t2p, parentOrAccepted(getScientific("Trichoneura hirtella Napper", Rank.SPECIES).node).usageKey);

    assertEquals(b1, getScientific("Blattaria P.Miller, 1754", Rank.GENUS).usageKey);
    assertEquals(b2, getScientific("Blattaria O.Kuntze, 1891", Rank.GENUS).usageKey);
    assertEquals(b3, getScientific("Blattaria Voet, 1806", Rank.GENUS).usageKey);
    assertEquals(b4, getScientific("Blattaria Weyenbergh, 1874", Rank.GENUS).usageKey);
  }

  /**
   * OTU & accepted Coccinella genus
   * https://github.com/gbif/checklistbank/issues/142
   * https://github.com/gbif/checklistbank/issues/163
   * <p>
   * 147=CoL
   * 148=BOLD
   * 149=WORMS
   * 150=Checklist of Beetles of Canada and Alaska
   * 151=Dyntaxa
   * 152=Dutch Species Register
   * 153=ITIS
   * 154=IRMNG
   * 155=ZooBank
   * 156=Plazi https://www.gbif.org/dataset/a5b00837-8dfa-46ec-828c-fbaffb41ec69
   * 157=Plazi https://www.gbif.org/dataset/c19789c1-c9be-4c54-b130-e1c8e861b2be
   * 158=Plazi https://www.gbif.org/dataset/f7175382-787c-4bb8-a95f-a6bb704becd0
   */
  @Test
  public void testStableIds2() throws Exception {
    int[] sourceKeys = new int[]{147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158};
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, sourceKeys);
    src.setSourceRank(147, Rank.KINGDOM);
    src.setSourceRank(151, Rank.GENUS);
    src.setSupragenericHomonymSource(147);

    // add Coccinella exclude configs for COL during rebuild only!!!
    cfg.homonymExclusions.put("Coccinella", Lists.newArrayList("Hydrophilidae", "Mollusca"));
    build(src);

    // 1 accepted Coccinella only
    NubUsage coccinella = getCanonical("Coccinella", Rank.GENUS, TaxonomicStatus.ACCEPTED);
    assertEquals("Coccinella Linnaeus, 1758", coccinella.parsedName.getScientificName());
    assertEquals("Linnaeus, 1758", coccinella.parsedName.authorshipComplete());

    assertTree("147 148 149 150 151 152 153 154 155 156 157 158.txt");

    int grac = getScientific("Gracilariaceae", Rank.FAMILY).usageKey;

    NubUsage u = getScientific("BOLD:ADR0965", Rank.UNRANKED);
    assertEquals("BOLD:ADR0965", u.parsedName.getScientificName());
    int otu = u.usageKey;

    // rebuild nub with additional sources!
    src = ClasspathSourceList.source(neoRepo.cfg, sourceKeys);
    src.setSourceRank(147, Rank.KINGDOM);
    src.setSupragenericHomonymSource(147);
    rebuild(src);

    // assert ids havent changed!
    assertEquals(grac, getScientific("Gracilariaceae", Rank.FAMILY).usageKey);
    assertEquals(otu, getScientific("BOLD:ADR0965", Rank.UNRANKED).usageKey);
  }

  @Test
  public void testStableIdsBasionymPlaceholder() throws Exception {
    int[] sourceKeys = new int[]{159};
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, sourceKeys);
    build(src);

    // basionym placeholder
    NubUsage bp = getScientific("? bracteata D.Don", Rank.UNRANKED);
    assertEquals("? bracteata D.Don", bp.parsedName.getScientificName());
    assertEquals(Origin.BASIONYM_PLACEHOLDER, bp.origin);
    int bpID = bp.usageKey;

    assertTree("159.txt");

    // rebuild nub with additional sources!
    src = ClasspathSourceList.source(neoRepo.cfg, sourceKeys);
    rebuild(src);

    // assert ids havent changed!
    assertEquals(bpID, getScientific("? bracteata D.Don", Rank.UNRANKED).usageKey);
  }

  /**
   * https://github.com/gbif/portal-feedback/issues/2732
   * 160=CoL
   * 161=IRMNG
   * 162=Plazi
   */
  @Test
  public void ixodes() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 160, 161, 162);
    src.setSourceRank(160, Rank.KINGDOM);
    src.setSourceRank(162, Rank.GENUS);
    src.setSourceIgnoreSynonyms(162, true);
    cfg.homonymExclusions.put("Ixodes", Lists.newArrayList("Aves"));
    build(src);

    // plazi genus is blocked, just the ticks one is left
    NubUsage u = getCanonical("Ixodes", Rank.GENUS);
    assertEquals("Ixodes Latreille, 1795", u.parsedName.canonicalNameComplete());
    assertEquals("Ixodes Latreille, 1795", u.parsedName.getScientificName());
    assertEquals("Ixodidae", parentOrAccepted(u.node).parsedName.getScientificName());

    // plazi species still there
    assertEquals("Ixodes granulatus Supino, 1897", getCanonical("Ixodes granulatus", Rank.SPECIES).parsedName.canonicalNameComplete());
  }

  /**
   * https://github.com/gbif/portal-feedback/issues/2732
   * 163=CoL
   * 164=WCVP
   * 165=WoRMS
   * 166=ITIS
   * 167=IRMNG
   * 168=IPNI
   * 169=ZooBank
   */
  @Test
  public void jerdonia() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 163, 164, 165, 166, 167, 168, 169);
    src.setSourceRank(163, Rank.KINGDOM);
    build(src);

    assertTree("163 164 165 166 167 168 169.txt");
  }


  /**
   * 170=CoL
   * 171=Danish National List
   */
  @Test
  public void speciesAuthorErrors() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 170, 171);
    src.setSourceRank(170, Rank.KINGDOM);
    build(src);

    assertTree("170 171.txt");
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-3024
   * 77=CoL
   * 78=IRMNG
   * 79=IOC Birds
   * 80=TAXREF
   * 81=IPNI
   */
  @Test
  public void testUpdateAuthors() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 77, 78, 79, 80, 81);
    src.setSourceRank(77, Rank.KINGDOM);
    src.setNomenclator(81);
    build(src);

    NubUsage u = getScientific("Oenanthe Vieillot, 1816", Rank.GENUS);
    assertEquals("Oenanthe Vieillot, 1816", u.parsedName.canonicalNameComplete());
    assertEquals("Oenanthe Vieillot, 1816", u.parsedName.getScientificName());

    u = getScientific("Oenanthe L.", Rank.GENUS);
    assertEquals("Oenanthe L.", u.parsedName.canonicalNameComplete());
    assertEquals("Oenanthe L.", u.parsedName.getScientificName());

    assertTree("77 78 79 80 81.txt");
  }

  @Test
  public void testUpdateClassification() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 3, 5, 7);
    src.setSourceRank(3, Rank.KINGDOM);
    build(src);

    // no authors in family names
    NubUsage fam = assertCanonical("Agaricaceae", "", Rank.FAMILY, Origin.SOURCE);
    NubUsage g = assertCanonical("Lepiota", Rank.GENUS, Origin.IMPLICIT_NAME, fam);
    NubUsage ls = assertCanonical("Lepiota seminuda", Rank.SPECIES, Origin.SOURCE, g);

    assertClassification(ls, "Lepiota", "Agaricaceae", "Agaricales", "Agaricomycetes", "Basidiomycota", "Fungi");

    // this genus should not be updated as its classification in source 7 contradicts the original one
    NubUsage b = assertCanonical("Berto", "Miller", Rank.GENUS, Origin.SOURCE);
    assertClassification(b, "Agaricales", "Agaricomycetes", "Basidiomycota", "Fungi");
  }

  @Test
  public void testCreateImplicitGenus() throws Exception {
    build(ClasspathSourceList.source(neoRepo.cfg, 1));

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
    build(ClasspathSourceList.source(neoRepo.cfg, 1));

    List<NubUsage> nudas = listCanonical("Lepiota nuda nuda");
    assertEquals(2, nudas.size());

    NubUsage var = null;
    NubUsage ssp = null;
    for (NubUsage u : nudas) {
      assertEquals(Origin.AUTONYM, u.origin);
      assertEquals("Lepiota nuda nuda", u.parsedName.canonicalName());
      if (u.rank == Rank.SUBSPECIES) {
        assertEquals("Lepiota nuda subsp. nuda", u.parsedName.getScientificName());
        ssp = u;
      } else if (u.rank == Rank.VARIETY) {
        assertEquals("Lepiota nuda var. nuda", u.parsedName.getScientificName());
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
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 3);
    src.setSourceRank(3, Rank.KINGDOM);
    build(src);

    assertNotNull(getCanonical("Animalia", Rank.KINGDOM));
    assertNotNull(getCanonical("Coleoptera", Rank.ORDER));
    assertNotNull(getCanonical("Poaceae", Rank.FAMILY));
  }

  /**
   * Make sure that species not snapping to any existing higher taxon get created under the incertae sedis kingdom
   * Also allow subsequent source to move a taxon from incertae sedis to somewhere else
   */
  @Test
  public void testIncertaeSedis() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 34, 105);
    build(src);

    assertTree("34 105.txt");
  }

  /**
   * Testing entire family Hymenopodidae from the Mantodea dataset:
   * http://dev.gbif.org/issues/browse/POR-2986
   */
  @Test
  public void testIncertaeSedisSynonyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 42);
    build(src);

    assertTree("42.txt");
  }

  @Test
  public void testColAdiantumSynonym() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 8);
    src.setSourceRank(8, Rank.PHYLUM);
    build(src);

    assertTree("8.txt");

    List<NubUsage> pedatums = listCanonical("Adiantum pedatum");
    assertEquals(4, pedatums.size());
    for (NubUsage u : pedatums) {
      System.out.println(u.parsedName.getScientificName());
      NubUsage p = parentOrAccepted(u.node);
      NameUsage nu = dao.readUsage(p.node, true);
      switch (u.parsedName.getScientificName()) {
        case "Adiantum pedatum":
          assertFalse(u.status.isSynonym());
          assertEquals("Adiantum", p.parsedName.canonicalName());
          break;
        case "Adiantum pedatum Forst.":
          assertTrue(u.status.isSynonym());
          assertFalse(p.status.isSynonym());
          assertEquals("Adiantum hispidulum Sw.", p.parsedName.getScientificName());
          assertEquals((Integer) 104, nu.getSourceTaxonKey());
          break;
        case "Adiantum pedatum A.Peter":
          assertTrue(u.status.isSynonym());
          assertFalse(p.status.isSynonym());
          assertEquals("Adiantum patens subsp. oatesii (Bak.) Schelpe", p.parsedName.getScientificName());
          assertEquals((Integer) 105, nu.getSourceTaxonKey());
          break;
        case "Adiantum pedatum Raddi":
          assertTrue(u.status.isSynonym());
          assertFalse(p.status.isSynonym());
          assertEquals("Adiantum brasiliense Raddi", p.parsedName.getScientificName());
          assertEquals((Integer) 106, nu.getSourceTaxonKey());
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
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 11, 12);
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
   * Also test what happens if a higher taxon exists twice with a slightly different classification in CoL.
   * E.g. class Jungermanniopsida
   * Suggest to keep the first occurrence
   */
  @Test
  public void testHomonyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 3, 2);
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
  public void testHomonym2() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 3, 2, 36);
    src.setSourceRank(3, Rank.KINGDOM);
    build(src);

    assertEquals(2, listCanonical("Trichoneura bontocensis").size());
    assertScientific("Trichoneura bontocensis Alexander, 1934", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
    assertScientific("Trichoneura bontocensis Perseus, 1999", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, null);

    assertEquals(2, listCanonical("Heliopyrgus willi").size());
    assertScientific("Heliopyrgus willi People, 1974", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
    assertScientific("Heliopyrgus willi Plötz, 1884", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, null);

    assertEquals(2, listCanonical("Meliopyrgus willi").size());
    assertScientific("Meliopyrgus willi People, 1974", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, null);
    assertScientific("Meliopyrgus willi Plötz, 1884", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
    assertTree("3 2 36.txt");
  }

  @Test
  public void testGenusHomonyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 29, 30, 31);
    src.setSourceRank(29, Rank.PHYLUM);
    build(src);

    NubUsage pyro = assertCanonical("Pyrostephos", "Moser, 1925", null, Rank.GENUS, Origin.SOURCE);
    assertCanonical("Mica micula", "Margulis, 1982", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, pyro);
    assertCanonical("Mica spectata", "", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, pyro);

    List<NubUsage> micas = listCanonical("Mica");
    assertEquals(5, micas.size());

    assertTree("29 30 31.txt");
  }

  /**
   * Exclude 2 families Limoniidae & Poaceae
   */
  @Test
  public void testExclusion() throws Exception {
    Map<Integer, List<RankedName>> exclusions = new HashMap<>();
    exclusions.put(2, Lists.newArrayList(
        new RankedName("Poaceae", Rank.FAMILY),
        new RankedName("Limoniidae", Rank.FAMILY)
    ));

    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, exclusions, 2);
    build(src);

    assertTree("2ex.txt");
  }

  @Test
  public void testHybrids() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 9);
    src.setSourceRank(9, Rank.PHYLUM);
    build(src);

    assertNotNull(getCanonical("Plantae", Rank.KINGDOM));
    assertCanonical("Adiantum", "", null, Rank.GENUS, Origin.IMPLICIT_NAME);
    assertCanonical("Asplenium", "", null, Rank.GENUS, Origin.IMPLICIT_NAME);

    assertCanonical("Adiantum capillus-veneris", "L.", null, Rank.SPECIES, Origin.SOURCE);
    assertCanonical("Asplenium adiantum-nigrum", "", null, Rank.SPECIES, Origin.IMPLICIT_NAME);
    assertCanonical("Asplenium adiantum-nigrum yuanum", "(Ching) Viane, Rasbach, Reichstein & Schneller", null, Rank.SUBSPECIES, Origin.SOURCE);
    assertCanonical("Adiantum moranii", "J.Prado", NamePart.SPECIFIC, Rank.SPECIES, Origin.SOURCE);

    assertNull(getCanonical("Asplenium adiantum nigrum × septentrionale", Rank.SPECIES));
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2874
   * As found in CoL as of june 2015. 4 times the same moss species name Fontinalis antipyretica with different authors, all accepted.
   * These are likely chresonyms that slipped through the COL editors attention.
   * This is illegal to the code rules, so just one should be accepted.
   * Pick one and make all others doubtful.
   * <p>
   * Abies taxifolia C.Presl
   * Abies taxifolia Drum. ex Gordon
   * Abies taxifolia Jeffr. ex Gordon
   * Abies taxifolia Raf.
   */
  @Test
  public void testMultipleAcceptedNames() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 10, 37, 38, 39);
    build(src);

    NubUsage genus = assertCanonical("Fontinalis", "", null, Rank.GENUS, Origin.IMPLICIT_NAME);
    int accepted = 0;
    int doubtful = 0;
    for (NubUsage c : children(genus.node)) {
      assertEquals(Rank.SPECIES, c.rank);
      assertEquals("Fontinalis antipyretica", c.parsedName.canonicalName());
      if (c.status == TaxonomicStatus.ACCEPTED) {
        accepted++;
      } else if (c.status == TaxonomicStatus.DOUBTFUL) {
        doubtful++;
      } else {
        fail("Fontinalis antipyretica species must be accepted or doubtful");
      }
    }
    assertEquals(1, accepted);
    assertEquals(3, doubtful);

    assertScientific("Abies pindrow (Royle ex D.Don) Royle", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
    assertScientific("Abies pindrow Spach", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, null);

    assertScientific("Abies taxifolia Raf.", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
    assertScientific("Abies taxifolia C.Presl", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, null);
    assertScientific("Abies taxifolia Drum. ex Gordon", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, null);
    assertNotExisting("Abies taxifolia Jeffr. ex Gordon", Rank.SPECIES);

    assertTree("10 37 38 39.txt");
  }

  /**
   * Avoid seeing a stackoverflow error when trying to persist missing genus or autonyms
   * with incomplete names missing a genus but that can be parsed.
   */
  @Test
  public void testIncompleteNames() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 24);
    src.setSourceRank(24, Rank.FAMILY);
    build(src);

    assertTrue(listCanonical("doliolum").isEmpty());
    assertTrue(listCanonical("Aster doliolum").isEmpty());
    assertTrue(listCanonical("Cichorium doliolum").isEmpty());
    assertTrue(listCanonical("Cichorium doliolum doliolum").isEmpty());

    assertTree("24.txt");
  }

  /**
   * CoL used to contain the genus Albizia twice within the plants as an accepted name (Fabaceae & Asteraceae).
   * http://www.catalogueoflife.org/col/details/species/id/17793647/source/tree
   * http://www.catalogueoflife.org/col/details/species/id/11468181/source/tree
   * The backbone should only contain one accepted genus in Fabaceae.
   * The Asteraceae one as doubtful.
   */
  @Test
  public void testAlbiziaCoL() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 13);
    src.setSourceRank(13, Rank.FAMILY);
    build(src);

    NubUsage fab = assertCanonical("Fabaceae", "", null, Rank.FAMILY, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
    NubUsage genus = assertCanonical("Albizia", "", null, Rank.GENUS, Origin.IMPLICIT_NAME, TaxonomicStatus.ACCEPTED, fab);
    NubUsage adianthifolia = assertCanonical("Albizia adianthifolia", "(Schum.) W.Wight", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, genus);

    assertCanonical("Albi minki", "W.Wight", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.DOUBTFUL, null);
    NubUsage tomentosa = assertCanonical("Albi tomentosa", "(Micheli) Standl.", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);

    // these are recombinations from the Albizia names above
    // But because they appear in the same source we do not convert them into synonyms
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
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 28);
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
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 14);
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
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 19);
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
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 18);
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
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 32, 33);
    src.setSourceRank(32, Rank.PHYLUM);
    src.setNomenclator(33);
    build(src);

    assertTree("32 33.txt");
  }

  /**
   * Avoid Plantae spec. and other indetermined names found in Pensoft sources.
   */
  @Test
  public void testSpecNames() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 44);
    build(src);

    assertTree("44.txt");
  }

  /**
   * Make sure families with different authorships dont get created twice.
   * We want unique names!
   */
  @Test
  public void testUniqueFamilies() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 45);
    src.setSourceRank(45, Rank.PHYLUM);
    build(src);

    assertScientific("Asteraceae", Rank.FAMILY, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);

    assertTree("45.txt");
  }

  @Test
  @Ignore("Manual test for profiling performance issues")
  public void testPerformance() throws Exception {
    NubSourceList src = new NubSourceList(new NubConfiguration());
    src.submitSources(Lists.newArrayList(
        new RandomSource(200000, Kingdom.ANIMALIA, neoRepo.cfg),
        new RandomSource(100, Kingdom.BACTERIA, neoRepo.cfg),
        new RandomSource(100, Kingdom.ARCHAEA, neoRepo.cfg),
        new RandomSource(20000, Kingdom.FUNGI, neoRepo.cfg),
        new RandomSource(50000, Kingdom.PLANTAE, neoRepo.cfg)));
    build(src);
  }

  /**
   * CoL contains canonical synonyms without authorship which are linked to an accepted taxon which has the same canonical name, but includes proper
   * authorship.
   * Ignore those synonyms as they are meaningless and cluttering.
   */
  @Test
  public void testAvoidCanonicalSynonym() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 17);
    build(src);
    assertEquals(1, listCanonical("Fuligo septica").size());
  }

  /**
   * Nub builds seem to have trouble if the given rank and the name parsed rank differ.
   * Make sure higher taxonomy does not suffer from that.
   * <p>
   * order Echiuroidea with family Ikedidae [family]
   * Araneae
   *
   * @throws Exception
   */
  @Test
  public void testHigherRanksWithAwkwardsSuffices() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 46);
    src.setSourceRank(46, Rank.KINGDOM);
    build(src);
    assertTree("46.txt");
  }

  /**
   * Assert behavior for various unparsable names.
   * There are no implicit names for unparsables, hence classification can miss the genus
   */
  @Test
  public void testUnparsables() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 47);
    build(src);

    assertTree("47.txt");
  }

  /**
   * Pro parte synonyms should exist as a single synonym node with multiple synonym relations
   */
  @Test
  public void testProParteSynonym() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 15, 16);
    build(src);

    NubUsage u = assertCanonical("Poa pubescens", "Lej.", null, Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.PROPARTE_SYNONYM, null);
    assertEquals(3, u.sourceIds.size());

    NameUsage nu = dao.readUsage(u.node, true);
    assertEquals("Poa pratensis L.", nu.getAccepted());

    List<Relationship> rels = Iterables.asList(u.node.getRelationships(RelType.PROPARTE_SYNONYM_OF, Direction.OUTGOING));
    Relationship acc = Iterables.single(u.node.getRelationships(RelType.SYNONYM_OF, Direction.OUTGOING));
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
   * In the synonymy here: http://cichorieae.e-taxonomy.net/portal/cdm_dataportal/taxon/209399b6-0d3c-4f5a-9f0d-b49ebe0f9403/synonymy
   * the Macrorhynchus humilis group (and all others below) are heterotypical synoyms of Agoseris apargioides, but they have homotypical
   * synonyms listed under them. The final synonym relationship from Agoseris apargioides var. maritima to Agoseris apargioides is therefore
   * a heterotypical one, not homotypic!
   */
  @Test
  @Ignore("write test")
  public void testHomoToHeteroTypicalSynonym() throws Exception {
  }


  @Test
  public void testBeeBasionyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 40);
    src.setSourceRank(40, Rank.PHYLUM);
    build(src);

    NubUsage nu = assertScientific("Amegilla atrocincta (Lepeletier)", Rank.SPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
    NameUsage u = getUsage(nu.node);
    assertEquals("Anthophora atrocincta Lepeletier, 1841", u.getBasionym());

    printTree();
    assertTree("40.txt");
  }

  /**
   * Neotetrastichodes flavus Girault, 1913 in the col takes priority and is a synonym of A. rieki.
   * Therefore all combinations of that species epithet become also synonyms including the formerly doubtfully accepted Aprostocetus flavus (Girault, 1913)
   */
  @Test
  public void testMultipleOriginals() throws Exception {
    // data from catalog of life=48, IRMNG=49
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 48, 50);
    build(src);

    printTree();

    assertTree("48 50.txt");
  }

  /**
   * Same as above, but this time Aprostocetus flavus is a properly accepted name in CoL and takes priority.
   * Therefore we keep 2 accepted taxa and not all names get merged.
   */
  @Test
  public void testMultipleOriginals2() throws Exception {
    // data from catalog of life=49, IRMNG=50
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 49, 50);
    build(src);

    printTree();

    assertTree("49 50.txt");
  }

  /**
   * Neotetrastichodes flavus is a synonym of Aprostocetus rieki, but also the basionym of Aprostocetus flavus.
   * Expect the basionym to be a synonym of Aprostocetus flavus if its present in different sources.
   * <p>
   * Leave other accepted species as it was - we could consider to merge them all into a single accepted name...
   */
  @Test
  public void testOverlappingBasionyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 51, 144);
    build(src);

    printTree();

    assertTree("51 144.txt");
  }

  /**
   * Eternal loop that keeps the shell up and running so one can conenct to it and issue queries.
   */
  private void runShell() throws InterruptedException {
    System.out.println("run shell forever ...");

    while (true) {
      Thread.sleep(1000);
    }
  }

  /**
   * Virus names are not parsable, do extra test to verify behavior
   */
  @Test
  public void testVirusNames() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 22, 23);
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
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 35);
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
   * Make sure frequent INFRASPECIES rank in col is parsed into real ranks
   */
  @Test
  public void testInfraspeciesRanks() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 53);
    build(src);

    assertCanonical("Lupinus sericeus flavus", Rank.SUBSPECIES, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
    assertCanonical("Lupinus sericeus jonesii", Rank.VARIETY, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);

    assertTree("52.txt");
  }


  /**
   * Avoid bad parent relationships or even loops.
   * 52=CoL
   * 54=ITIS
   * 55=IPNI
   */
  @Test
  public void testColItisIpniMerger() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 52, 54, 55);
    src.setNomenclator(55);
    build(src);

    assertTree("52 54 55.txt");
  }

  /**
   * Test to avoid self loops in CoL autonyms, caused by bad basionym grouping.
   * 56=CoL
   * 57=IRMNG
   * 58=IF
   * <p>
   * Parmelia tiliacea is created as an implicit name because IRMNG uses "Parmelia tiliacea sensu auct. brit.p.p." which gets ignored as a concept name
   */
  @Test
  public void testColIfAutonym() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 56, 57, 58);
    src.setNomenclator(58);
    build(src);

    assertTree("56 57 58.txt");
  }

  /**
   * Test nested infraspecific source taxa.
   * In the nub we do not want nested infraspecific taxa, but attach all accepted infraspecific names to the species directly
   */
  @Test
  public void testInfraspecificTrees() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 59);
    build(src);

    assertTree("59.txt");
  }

  @Test
  public void testInfraspecificBasionymGrouping() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 60, 145);
    build(src);

    assertTree("60 145.txt");
  }

  /**
   * 61=CoL
   * 62=IPNI
   */
  @Test
  public void testAutonymColBasionym() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 61, 62);
    src.setNomenclator(62);
    build(src);

    assertTree("61 62.txt");
  }

  /**
   * Make sure names coming in from sources without a kingdom or classification still contains to existing names in a proper kingdom
   * E.g. Toxostoma rufum in PalaeoDB has no classification and "should" be merged with existing animal Toxostoma rufum (Linnaeus, 1758)
   * <p>
   * Examples here use Parmelina quercina for tests
   */
  @Test
  public void testInsertaeSedisDuplicates() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 58, 63);
    build(src);

    assertTree("58 63.txt");
  }

  /**
   * Make sure aggregate names such as Achillea millefolium at rank SPECIES_AGGREGATE which are binomials are treated as species.
   * But ignore names at higher ranks such as Achillea pillefolium INFRAGENERIC_NAME.
   */
  @Test
  public void testAggregates() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 64);
    build(src);

    assertTree("64.txt");
  }

  /**
   * Make sure subgenera in binomial names are removed.
   */
  @Test
  public void testSubgenusRemoval() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 65);
    build(src);

    assertTree("65.txt");
  }

  /**
   * Some plazi datasets (but also others) get indexed badly by us cause they contain bad synonymies linking a genus to a species.
   * That corrupts the entire, normalized classification and nub builds must be able to deal with these inverted, bad classifications.
   */
  @Test
  public void testBadPlaziHierarchy() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 66);
    build(src);

    assertTree("66.txt");
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2824
   */
  @Test
  public void testBasionymEpithetStemming() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 67, 142);
    build(src);

    assertTree("67 142.txt");
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-2989
   */
  @Test
  public void testMantodeaBasionyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 68);
    build(src);

    assertTree("68.txt");
  }


  /**
   * http://dev.gbif.org/issues/browse/POR-3024
   * 70=COL
   * 71=Official Lists and Indexes of Names in Zoology
   * 72=ITIS
   * 73=IRMNG
   * 74=Clements Birds
   * 75=IOC Birds
   * 76=IPNI
   */
  @Test
  public void testCardinalis() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 70, 71, 72, 73, 74, 75, 76);
    src.setNomenclator(76);
    build(src);

    assertTree("70 71 72 73 74 75 76.txt");
  }

  private NubUsage create(int id, Rank rank, Kingdom kingdom, String sciname) {
    NubUsage u = new NubUsage();
    u.node = dao.createTaxon();
    u.usageKey = id;
    u.rank = rank;
    u.kingdom = kingdom;
    u.parsedName = PARSER.parseQuietly(sciname, rank);
    dao.store(u);
    return u;
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-3060
   * 70=COL
   * 71=Official Lists and Indexes of Names in Zoology
   * 72=ITIS
   * 73=IRMNG
   * 74=Clements Birds
   * 75=IOC Birds
   * 76=IPNI
   */
  @Test
  public void testCardinalisWithExistingNub() throws Exception {
    // create existing genus & species from 2013 backbone
    tx = dao.beginTx();
    create(3241527, Rank.GENUS, Kingdom.ANIMALIA, "Cardinalis Bonaparte, 1838");
    create(2490383, Rank.GENUS, Kingdom.ANIMALIA, "Cardinalis Bonaparte, 1831");
    create(3232102, Rank.GENUS, Kingdom.PLANTAE, "Cardinalis Fabricius, 1759");
    create(2490384, Rank.SPECIES, Kingdom.ANIMALIA, "Cardinalis cardinalis (Linnaeus, 1758)");
    create(4846779, Rank.SPECIES, Kingdom.ANIMALIA, "Cardinalis cardinalis (Linnaeus, 1758)");
    create(7191770, Rank.SUBSPECIES, Kingdom.ANIMALIA, "Cardinalis cardinalis subsp. cardinalis");
    create(5230886, Rank.SUBSPECIES, Kingdom.ANIMALIA, "Cardinalis cardinalis clintoni (Banks, 1963)");

    // rebuild nub
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 70, 71, 72, 73, 74, 75, 76);
    src.setNomenclator(76);
    rebuild(src);

    assertTree("70 71 72 73 74 75 76.txt");
    assertKey("Cardinalis Fabr.", Rank.GENUS, Kingdom.PLANTAE, 3232102);
    assertKey("Cardinalis Bonaparte, 1838", Rank.GENUS, Kingdom.ANIMALIA, 3241527);
    assertKey("Cardinalis cardinalis (Linnaeus, 1758)", Rank.SPECIES, Kingdom.ANIMALIA, 2490384);
  }


  /**
   * http://dev.gbif.org/issues/browse/POR-3063
   * 82=COL
   * 83=ITIS
   * 84=DynTaxa
   * 85=IRMNG
   */
  @Test
  public void testNameDuplication() throws Exception {
    // rebuild nub
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 82, 83, 84, 85);
    src.setSourceRank(82, Rank.KINGDOM);
    build(src);

    assertTree("82 83 84 85.txt");
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-3069
   * Austrorhynchus pectatus pectatus f. Sporn looks already wrong in WoRMS but even worse in GBIF as: Austrorhynchus pectatus null pectatus
   * http://www.marinespecies.org/aphia.php?p=taxdetails&id=154959
   * <p>
   * 86=WoRMS
   * 87=TAXREF
   */
  @Test
  public void testNullNames() throws Exception {
    // rebuild nub
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 86, 87);
    build(src);

    assertTree("86 87.txt");
  }


  /**
   * http://dev.gbif.org/issues/browse/POR-3070
   * <p>
   * 88=CoL
   * 89=Orthoptera SPecies File
   * 90=IRMNG
   */
  @Test
  public void testRedundantHomonyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 88, 89, 90);
    src.setSourceRank(88, Rank.KINGDOM);
    build(src);

    assertTree("88 89 90.txt");
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-3169
   * <p>
   * 104=Palaeodb
   */
  @Test
  public void avoidKingdomSpeciesSynonyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 104);
    build(src);

    assertTree("104.txt");
  }

  /**
   * http://dev.gbif.org/issues/browse/PF-2600
   */
  @Test
  public void testGenusNormilization() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 106, 107);
    build(src);

    assertTree("106 107.txt");
  }

  /**
   * Test hyphens in genus name.
   * Only one of these 2 should exist, preferrably the Camel-Case
   * Drake-brockmania
   * Drake-Brockmania
   * <p>
   * This should be allowed to also exist but always as a synonym of the above!
   * Drakebrockmania
   */
  @Test
  @Ignore("NubBuilder needs code change first")
  public void testDrakeBrockmania() throws Exception {

    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 108, 109, 110);
    src.setSourceRank(108, Rank.PHYLUM);
    build(src);

    assertTree("108 109 110.txt");
  }

  @Test
  public void testColInfraspecificRank() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 111);
    build(src);

    assertTree("111.txt");
    assertScientific("Collotheca heptabrachiata var. molundica", Rank.VARIETY, Origin.SOURCE, TaxonomicStatus.ACCEPTED, null);
  }

  /**
   * by introducing the type specimen checklist we see many apparently accepted names coming in via different kingdoms.
   * Make sure these get merged if the name matches incl authorship!
   */
  @Test
  public void testDifferentKingdoms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 112, 113);
    // type specimen list
    src.setSourceRank(113, Rank.SPECIES);
    build(src);

    assertTree("112 113.txt");
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-3168
   */
  @Test
  public void testBadSpeciesSynonyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 114);
    src.setSourceRank(114, Rank.PHYLUM);
    build(src);

    assertTree("114.txt");
  }

  /**
   * http://dev.gbif.org/issues/browse/POR-3213
   */
  @Test
  public void testNamePublishedIn() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 115);
    src.setSourceRank(115, Rank.PHYLUM);
    build(src);

    NubUsage u = assertCanonical("Archelytron", Rank.GENUS, Origin.SOURCE, null);
    assertEquals("Insektenfunde aus den Goldlauterer Schichten des Thüringer Waldes. Hallesches Jahrbuch für Mitteldeutsche Erdgeschichte 1:241-258. 1952", u.publishedIn);

    u = assertCanonical("Blatta hyperborea", Rank.SPECIES, Origin.SOURCE, null);
    assertEquals("O. Heer. 1870. Die Miocene flora und fauna Spitzbergens. Kongliga Svenska Vetenskaps-Akademiens Handlingar 8(7):1-98", u.publishedIn);
  }

  /**
   * https://github.com/gbif/checklistbank/issues/25
   */
  @Test
  @Ignore
  public void testNewSpeciesInHomonymGenus() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 116, 117);
    src.setSourceRank(116, Rank.PHYLUM);
    src.setSourceRank(117, Rank.GENUS);
    src.setNomenclator(117);
    build(src);

    assertTree("116 117.txt");
  }

  /**
   * Allow OTU names in the backbone, e.g. from BOLD & UNITE
   */
  @Test
  public void testOtuNames() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 118, 119);
    src.setSourceRank(118, Rank.PHYLUM);
    src.setSourceRank(119, Rank.GENUS);
    build(src);

    NubUsage u = assertScientific("BOLD:ABW2624", Rank.SPECIES, Origin.SOURCE, null, null);
    assertEquals(NameType.OTU, u.parsedName.getType());

    u = assertScientific("SH002390.07FU", Rank.UNRANKED, Origin.SOURCE, null, null);
    assertEquals(NameType.OTU, u.parsedName.getType());

    assertTree("118 119.txt");
  }

  /**
   * 120 = COL
   * 121 = WORMS
   *
   * @throws Exception
   */
  @Test
  @Ignore("needs implemented")
  public void avoidProparteFamilyHomonyms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 120, 121);
    src.setSourceRank(120, Rank.PHYLUM);

    build(src);

    assertTree("120 121.txt");
  }

  /**
   * Make sure blacklisted names dont make it into the backbone
   * https://github.com/gbif/checklistbank/issues/47
   */
  @Test
  public void testBlacklist() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 122);
    build(src);

    assertTree("122.txt");
  }

  /**
   * https://github.com/gbif/checklistbank/issues/30
   */
  @Test
  public void test30() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 123, 124);
    build(src);

    assertTree("123 124.txt");
  }

  /**
   * Verify canonical name building.
   * Subspecies in zoology should not have a marker
   */
  @Test
  public void testNameFormatting() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 125);
    build(src);

    NubUsage u = assertCanonical("Puma yagouaroundi chilensis", Rank.SUBSPECIES, Origin.SOURCE, null);
    assertEquals(Rank.SUBSPECIES, u.parsedName.getRank());
    assertEquals("Puma yagouaroundi chilensis L.", u.parsedName.getScientificName());

    assertTree("125.txt");
  }

  /**
   * Verify binomials with higher rank
   * and abbreviated namess
   * https://github.com/gbif/checklistbank/issues/62
   */
  @Test
  public void testAbbrevNames() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 126);
    build(src);
    assertTree("126.txt");
  }

  /**
   * Test WoRMS case of badly nested varieties combined with a duplicate autonym
   * leading to node NotFoundException
   * <p>
   * 127=CoL
   * 128=WoRMS
   * <p>
   * The main error comes from bad normalization of the WoRMS DwCA and should be tested in the Normalizer.
   * But the nub build should be gracefully dealing with such data as it is currently present in CLB.
   */
  @Test
  public void testIllegalVarWorms() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 127, 128);
    src.setSourceRank(127, Rank.KINGDOM);
    build(src);
    assertTree("127 128.txt");
  }

  @Test
  public void testBlacklistedNames() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 129);
    build(src);
    assertTree("129.txt");
  }

  @Test
  public void testHomonymFamiliesPatch() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 130, 131, 132);
    src.setSourceRank(130, Rank.PHYLUM); // backbone patch
    src.setSourceRank(131, Rank.PHYLUM); // CoL
    src.setSupragenericHomonymSource(130);
    build(src);
    assertTree("130 131 132.txt");
  }

  /**
   * https://github.com/gbif/checklistbank/issues/93
   */
  @Test
  public void testColHomonymGenera() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 139, 140, 141);
    src.setSourceRank(139, Rank.PHYLUM); // backbone patch
    src.setSourceRank(140, Rank.PHYLUM); // CoL
    //src.setSupragenericHomonymSource(140);
    build(src);
    assertTree("139 140 141.txt");
  }

  @Test
  public void testClementsDups() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 133, 134);
    src.setSourceRank(133, Rank.PHYLUM); // CoL
    src.setSourceRank(134, Rank.PHYLUM); // Clements Birdlist
    build(src);
    assertTree("133 134.txt");
  }

  /**
   * https://github.com/gbif/checklistbank/issues/88
   */
  @Test
  public void testLinnaeus() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 135);
    src.setSourceRank(135, Rank.PHYLUM);
    build(src);
    assertTree("135.txt");
  }

  /**
   * https://github.com/gbif/checklistbank/issues/88
   */
  @Test
  public void testBoldPlaceholder() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 136);
    build(src);
    assertTree("136.txt");
  }

  /**
   * https://github.com/gbif/checklistbank/issues/91
   */
  @Test
  public void testSpeciesPatch() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 137, 138);
    src.setSourceRank(137, Rank.PHYLUM); // backbone patch
    src.setSourceRank(138, Rank.PHYLUM); // CoL
    src.setSupragenericHomonymSource(137);
    build(src);
    assertTree("137 138.txt");
  }

  @Test
  public void misapplied() throws Exception {
    ClasspathSourceList src = ClasspathSourceList.source(neoRepo.cfg, 172);
    build(src);
    assertTree("172.txt");
  }

  /**
   * For profiling memory usage of nub builds
   */
  @Test
  @Ignore("for manual profiling only")
  public void testMemFootprint() throws Exception {
    List<RandomSource> sources = Lists.newArrayList();
    for (int x = 0; x < 1000; x++) {
      sources.add(new RandomSource(50, Kingdom.ANIMALIA, neoRepo.cfg));
      sources.add(new RandomSource(30, Kingdom.PLANTAE, neoRepo.cfg));
      sources.add(new RandomSource(20, Kingdom.FUNGI, neoRepo.cfg));
    }

    NubSourceList src = new NubSourceList(new NubConfiguration());
    src.submitSources(sources);
    build(src);
  }

  /**
   * builds a new nub and keeps dao open for further test queries.
   */
  private void build(NubSourceList src, @Nullable File treeOutput) throws Exception {
    Stopwatch watch = Stopwatch.createUnstarted();
    NubBuilder nb = NubBuilder.create(dao, src, IdLookupImpl.temp().load(Lists.<LookupUsage>newArrayList()), 10, cfg);
    nb.setCloseDao(false);
    try {
      nb.run();
    } catch (AssertionError e) {
      printTree();
      throw e;
    }
    log("Nub build completed in %sms", watch.elapsed(TimeUnit.MILLISECONDS));

    tx = dao.beginTx();
    dao.logAll();
    if (treeOutput != null) {
      printTree(treeOutput);
    } else {
      printTree();
    }

    // assert we have only ever 8 root taxa - the kingdoms
    assertEquals(Kingdom.values().length, countRoot());

    // assert we have unique ids
    assertUniqueIds();
    log("Core test completed in %sms", watch.elapsed(TimeUnit.MILLISECONDS));
  }

  private void build(NubSourceList src) throws Exception {
    build(src, null);
  }

  private void assertUniqueIds() {
    Set<Integer> keys = Sets.newHashSet();
    for (Node n : Iterators.loop(dao.allTaxa())) {
      NubUsage u = dao.readNub(n);
      if (keys.contains(u.usageKey)) {
        System.err.println(u);
        fail("Nub keys not unique: " + u.usageKey);
      } else {
        keys.add(u.usageKey);
      }
    }
  }

  private void rebuild(NubSourceList src) throws Exception {
    IdLookupImpl previousIds = IdLookupImpl.temp().load(allNodes(dao));
    tx.close();
    dao.close();
    // new, empty DAO
    dao = UsageDao.temporaryDao(100);
    NubBuilder nb = NubBuilder.create(dao, src, previousIds, previousIds.getKeyMax() + 1, cfg);
    nb.setCloseDao(false);
    nb.run();

    tx = dao.beginTx();
    printTree();

    // assert we have only ever 8 root taxa - the kingdoms
    assertEquals(Kingdom.values().length, countRoot());
    // assert we have unique ids
    assertUniqueIds();
  }

  /**
   * Read old ids from existing, open DAO
   */
  private static Iterable<LookupUsage> allNodes(final UsageDao dao) {
    return () -> StreamUtils.stream(dao.allTaxa())
        .map(n -> {
          NubUsage u = dao.readNub(n);
          return new LookupUsage(u.usageKey, ObjectUtils.coalesce(u.parsedName.canonicalName(), u.parsedName.getScientificName()), u.parsedName.getAuthorship(), u.parsedName.getYear(), u.rank, u.status, u.kingdom, false);
        }).iterator();
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

  private NubUsage assertCanonical(String canonical, @Nullable String authorship, @Nullable NamePart notho, Rank rank, Origin origin, @Nullable TaxonomicStatus status, @Nullable NubUsage parent, NameUsageIssue... issues) {
    return assertCanonical(canonical, authorship, notho, rank, null, origin, status, parent, issues);
  }

  private NubUsage assertCanonical(String canonical, @Nullable String authorship, @Nullable NamePart notho, Rank rank, Kingdom k, Origin origin, @Nullable TaxonomicStatus status, @Nullable NubUsage parent, NameUsageIssue... issues) {
    canonical = SciNameNormalizer.normalize(canonical);
    NubUsage u = getCanonical(canonical, rank, k);
    assertNub(u, canonical, authorship, notho, rank, origin, status, parent);
    if (k != null) {
      assertEquals("wrong kingdom " + k, k, u.kingdom);
    }
    assertEquals("wrong canonical name for " + canonical, canonical, UsageDao.canonicalOrScientificName(u.parsedName));
    for (NameUsageIssue issue : issues) {
      assertTrue("missing issue " + issue, u.issues.contains(issue));
    }
    return u;
  }

  private NubUsage assertScientific(String sciname, Rank rank, Origin origin, @Nullable TaxonomicStatus status, @Nullable NubUsage parent) {
    NubUsage u = getScientific(sciname, rank);
    assertNub(u, sciname, null, null, rank, origin, status, parent);
    assertEquals("wrong scientific name for " + sciname, sciname, u.parsedName.getScientificName());
    return u;
  }

  private NubUsage assertKey(String sciname, Rank rank, Kingdom kingdom, int key) {
    NubUsage u = getScientific(sciname, rank, kingdom);
    assertEquals("wrong key for " + sciname, key, u.usageKey);
    assertEquals("wrong scientific name for " + sciname, sciname, u.parsedName.getScientificName());
    assertEquals("wrong kingdom for " + sciname, kingdom, u.kingdom);
    assertEquals("wrong rank for " + sciname, rank, u.rank);
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
      assertEquals("wrong parent " + p2.parsedName.canonicalNameComplete() + " for " + name, p2.node, parent.node);
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
    canonical = SciNameNormalizer.normalize(canonical);

    List<NubUsage> usages = Lists.newArrayList();
    for (Node n : Iterators.loop(dao.getNeo().findNodes(Labels.TAXON, NeoProperties.CANONICAL_NAME, canonical))) {
      usages.add(get(n));
    }
    return usages;
  }

  private List<NubUsage> listSynonyms(NubUsage acc) {
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
    for (Node n : Iterators.loop(dao.getNeo().findNodes(Labels.TAXON, NeoProperties.SCIENTIFIC_NAME, sciname))) {
      usages.add(get(n));
    }
    return usages;
  }

  private NubUsage getScientific(String sciname, Rank rank, Kingdom kingdom) {
    return getOne(listScientific(sciname), rank, kingdom, null, sciname);
  }

  private NubUsage getScientific(String sciname, Rank rank) {
    return getOne(listScientific(sciname), rank, null, null, sciname);
  }

  private NubUsage getCanonical(String canonical, Rank rank) {
    return getOne(listCanonical(canonical), rank, null, null, canonical);
  }

  private NubUsage getCanonical(String canonical, Rank rank, TaxonomicStatus status) {
    return getOne(listCanonical(canonical), rank, null, status, canonical);
  }

  private NubUsage getCanonical(String canonical, Rank rank, Kingdom k) {
    return getOne(listCanonical(canonical), rank, k, null, canonical);
  }

  private NubUsage getCanonical(String canonical, Rank rank, @Nullable Kingdom k, @Nullable TaxonomicStatus status) {
    return getOne(listCanonical(canonical), rank, k, status, canonical);
  }

  private NubUsage getOne(List<NubUsage> usages, Rank rank, @Nullable Kingdom k, @Nullable TaxonomicStatus status, String name) {
    Iterator<NubUsage> iter = usages.iterator();
    while (iter.hasNext()) {
      NubUsage u = iter.next();
      if (u.rank != rank) {
        iter.remove();
      } else if (k != null && u.kingdom != k) {
        iter.remove();
      } else if (status != null && u.status != status) {
        iter.remove();
      }
    }
    if (usages.isEmpty()) {
      return null;
    } else if (usages.size() == 1) {
      return usages.get(0);
    }
    throw new IllegalStateException("Too many usages for " + status + " " + rank + " " + name);
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
    return Iterators.count(dao.getNeo().findNodes(Labels.TAXON));
  }

  private long countSpecies() {
    return Iterators.count(dao.getNeo().findNodes(Labels.SPECIES));
  }

  private long countGenera() {
    return Iterators.count(dao.getNeo().findNodes(Labels.GENUS));
  }

  private long countRoot() {
    return Iterators.count(dao.getNeo().findNodes(Labels.ROOT));
  }

  class TreeAsserter implements StartEndHandler {
    private Iterator<NubNode> treeIter;

    public TreeAsserter(NubTree tree) {
      treeIter = tree.iterator();
    }

    @Override
    public void start(Node n) {
      NubNode expected = treeIter.next();
      String name = (String) n.getProperty(NeoProperties.SCIENTIFIC_NAME);
      assertEquals(expected.name, name);
      assertEquals("Basionym flag wrong for " + name, expected.basionym, n.hasLabel(Labels.BASIONYM));
    }

    @Override
    public void end(Node n) {
    }

    public boolean completed() {
      return !treeIter.hasNext();
    }
  }

  private void assertTree(String filename) throws IOException {
    System.out.println("assert tree from " + filename);
    NubTree expected = NubTree.read("trees/" + filename);

    // compare trees
    assertEquals("Number of roots differ", expected.getRoot().children.size(), Iterators.count(dao.allRootTaxa()));
    TreeAsserter treeAssert = new TreeAsserter(expected);
    TreeWalker.walkTree(dao.getNeo(), true, treeAssert);
    assertTrue("There should be more taxa", treeAssert.completed());

    // verify all nodes are walked in the tree and contains the expected numbers
    long neoCnt = Iterables.count(dao.getNeo().getAllNodes());
    // pro parte nodes are counted multiple times, so expected count can be higher than pure number of nodes - but never less!
    System.out.println("expected nodes: " + expected.getCount());
    System.out.println("counted nodes: " + neoCnt);
    assertTrue(expected.getCount() >= neoCnt);
  }

  private void printTree() throws Exception {
    Writer writer = new PrintWriter(System.out);
    dao.printTree(writer, GraphFormat.TEXT);
  }

  private void printTree(File f) throws Exception {
    Writer writer = new FileWriter(f);
    dao.printTree(writer, GraphFormat.TEXT);
  }

}