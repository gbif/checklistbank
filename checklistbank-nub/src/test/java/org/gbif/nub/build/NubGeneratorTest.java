package org.gbif.nub.build;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.service.mybatis.ParsedNameServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.model.Usage;
import org.gbif.nameparser.NameParser;
import org.gbif.nub.utils.CacheUtils;
import org.gbif.utils.file.FileUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore("Work in progress - markus")
public class NubGeneratorTest {

  private static final Logger LOG = LoggerFactory.getLogger(NubGeneratorTest.class);

  private final JsonMockService batchService = new JsonMockService();
  private final DatasetMockService datasetService = new DatasetMockService();
  private ParsedNameServiceMyBatis nameService;
  private int nextNameId;
  private NameParser parser = new NameParser();
  private NubWriter nubWriter;
  private NubReporter nubReporter;

  @Before
  public void setupMocks() throws IOException {
    nextNameId = 100;

    nubWriter = mock(NubWriter.class);

    nubReporter = new NubReporter(FileUtils.createTempDir());

    nameService = mock(ParsedNameServiceMyBatis.class);
    when(nameService.createOrGet(anyString())).thenAnswer(new Answer<ParsedName>() {
      @Override
      public ParsedName answer(InvocationOnMock invocation) throws Throwable {
        String name = (String) invocation.getArguments()[0];
        ParsedName pn = parser.parse(name);
        pn.setScientificName(name);
        pn.setKey(nextNameId++);
        return pn;
      }
    });
  }

  @Test
  public void testBuild() throws IOException {
    datasetService.setKeys(NubSourceDataExporter.COL, NubSourceDataExporter.HOMONYMS, NubSourceDataExporter.MAMMALS,
      NubSourceDataExporter.TOORSACORRIDOR);
    NubGenerator gen = new NubGenerator(batchService, datasetService, datasetService, nameService, null, nubWriter, nubReporter);
    gen.build();

    StringWriter w = new StringWriter();
    gen.getReporter().writeReport(w);
    LOG.info(w.toString());
  }

  @Test
  public void testOneSmallList() throws IOException {
    datasetService.setKeys(NubSourceDataExporter.TOORSACORRIDOR);
    NubGenerator gen = new NubGenerator(batchService, datasetService, datasetService, nameService, null, nubWriter, nubReporter);
    // use higher taxonomy
    ChecklistCache nub = gen.build(NubSourceDataExporter.TOORSACORRIDOR);

    StringWriter w = new StringWriter();
    gen.getReporter().writeReport(w);
    LOG.info(w.toString());

    assertUsageExistsOnce(nub, "Plantae", Rank.KINGDOM, TaxonomicStatus.ACCEPTED, 0);
    assertUsageExistsOnce(nub, "Magnoliophyta", Rank.PHYLUM, TaxonomicStatus.ACCEPTED, 114524875);
    Usage u = assertUsageExistsOnce(nub, "Euonymus", Rank.GENUS, TaxonomicStatus.ACCEPTED, 114524927);

    LinneanClassification cl = new NameUsageMatch();
    cl.setKingdom("Plantae");
    cl.setPhylum("Magnoliophyta");
    cl.setClazz("Magnoliopsida");
    cl.setOrder("Celastrales");
    cl.setFamily("Celastraceae");
    cl.setGenus("Euonymus");
    assertClassification(nub, u, "Celastraceae", cl);

    // implicit names
    u = assertUsageExistsOnce(nub, "Drakonia", Rank.GENUS, TaxonomicStatus.ACCEPTED, -1);
    cl.setKingdom("Plantae");
    cl.setPhylum("Magnoliophyta");
    cl.setClazz("Magnoliopsida");
    cl.setOrder("Aquidfoliales");
    cl.setGenus("Drakonia");
    assertClassification(nub, u, "Aquidfoliales", cl);

    u = assertUsageExistsOnce(nub, "Drakoniata nospecies subsp. nospecies", Rank.SUBSPECIES, TaxonomicStatus.ACCEPTED,
      -1);
    u = assertUsageExistsOnce(nub, "Drakoniata nospecies subsp. europaea L.", Rank.SUBSPECIES, TaxonomicStatus.ACCEPTED,
      -102);
    cl.setKingdom("Plantae");
    cl.setPhylum("Magnoliophyta");
    cl.setClazz("Magnoliopsida");
    cl.setOrder("Aquidfoliales");
    cl.setGenus("Drakoniata");
    assertClassification(nub, u, "Drakoniata nospecies", cl);

    // test autonyms
    u = assertUsageExistsOnce(nub, "Drakonia gibtsnicht var. gibtsnicht", Rank.VARIETY, TaxonomicStatus.ACCEPTED, -1);
    cl.setKingdom("Plantae");
    cl.setPhylum("Magnoliophyta");
    cl.setClazz("Magnoliopsida");
    cl.setOrder("Aquidfoliales");
    cl.setGenus("Drakonia");
    assertClassification(nub, u, "Drakonia gibtsnicht L.", cl);


  }

  /**
   * Merging the classification from 2 checklists with the original, first checklist having holes in the higher
   * taxonomy which can be "patched" by data from the second list.
   */
  @Test
  public void testClassificationUpdate() throws IOException {
    final UUID patchList = UUID.fromString("12345678-1234-1234-1234-123456789001");
    datasetService.setKeys(NubSourceDataExporter.TOORSACORRIDOR, patchList);
    NubGenerator gen = new NubGenerator(batchService, datasetService, datasetService, nameService, null, nubWriter, nubReporter);
    // use higher taxonomy
    ChecklistCache nub = gen.build(NubSourceDataExporter.TOORSACORRIDOR);

    StringWriter w = new StringWriter();
    gen.getReporter().writeReport(w);
    LOG.info(w.toString());

    final Usage erfundenidae = assertUsageExistsOnce(nub, "Erfundenidae", Rank.FAMILY, TaxonomicStatus.ACCEPTED, -201);

    LinneanClassification cl = new NameUsageMatch();
    cl.setKingdom("Plantae");
    cl.setPhylum("Magnoliophyta");
    cl.setClazz("Magnoliopsida");
    cl.setOrder("Rhamnales");
    cl.setFamily("Erfundenidae");
    assertClassification(nub, erfundenidae, "Rhamnales", cl);

    // the genus Erfunden is linked to family Erfundenaceae in the main source
    // it is also linked to order Asterales in another checklist
    // We could update the family Erfundenaceae to the Asterales order, but this is considered too risky for now
    final Usage erfunden = assertUsageExistsOnce(nub, "Erfunden", Rank.GENUS, TaxonomicStatus.ACCEPTED, -300);
    final Usage erfundenaceae = assertUsageExistsOnce(nub, "Erfundenaceae", Rank.FAMILY, TaxonomicStatus.ACCEPTED, -200);
    final Usage asterales = assertUsageExistsOnce(nub, "Asterales", Rank.ORDER, TaxonomicStatus.ACCEPTED, 114524912);

    assertEquals(asterales.key, erfunden.parentKey);

    cl = new NameUsageMatch();
    cl.setKingdom("Plantae");
    cl.setPhylum("Magnoliophyta");
    cl.setClazz("Magnoliopsida");
    cl.setOrder("Asterales");
    cl.setGenus("Erfunden");
    assertClassification(nub, erfunden, "Asterales", cl);

  }

  @Test
  public void testCoLPlusOneSmallList() throws IOException {
    datasetService.setKeys(NubSourceDataExporter.COL, NubSourceDataExporter.TOORSACORRIDOR);
    NubGenerator gen = new NubGenerator(batchService, datasetService, datasetService, nameService, null, nubWriter, nubReporter);
    // use only the CoL higher taxonomy
    ChecklistCache nub = gen.build(NubSourceDataExporter.COL);

    StringWriter w = new StringWriter();
    gen.getReporter().writeReport(w);
    LOG.info(w.toString());

    assertUsageExistsOnce(nub, "Plantae", Rank.KINGDOM, TaxonomicStatus.ACCEPTED, 0);
    assertUsageDoesNotExists(nub, "Magnoliophyta");
    assertUsageExistsOnce(nub, "Tracheophyta", Rank.PHYLUM, TaxonomicStatus.ACCEPTED, 125413264);
    assertUsageExistsOnce(nub, "Celastraceae", Rank.FAMILY, TaxonomicStatus.ACCEPTED, 125420149);

    LinneanClassification cl = new NameUsageMatch();
    cl.setKingdom("Plantae");
    cl.setPhylum("Tracheophyta");
    cl.setClazz("Magnoliopsida");
    cl.setOrder("Celastrales");
    cl.setFamily("Celastraceae");
    cl.setGenus("Euonymus");
    // this is not the CoL source id, cause the CoL for tests only has families and above!
    Usage u = assertUsageExistsOnce(nub, "Euonymus", Rank.GENUS, TaxonomicStatus.ACCEPTED, 114524927);
    assertClassification(nub, u, "Celastraceae", cl);
  }

  @Test
  public void testHomonyms() throws IOException {
    datasetService
      .setKeys(NubSourceDataExporter.COL, NubSourceDataExporter.HOMONYMS, NubSourceDataExporter.TOORSACORRIDOR);
    NubGenerator gen = new NubGenerator(batchService, datasetService, datasetService, nameService, null, nubWriter, nubReporter);
    // use only the CoL higher taxonomy
    ChecklistCache nub = gen.build(NubSourceDataExporter.COL);

    StringWriter w = new StringWriter();
    gen.getReporter().writeReport(w);
    LOG.info(w.toString());

    // 2 valid Oenanthe genera
    Usage oPlant = nub.findUsages("Oenanthe Linnaeus, 1753").get(0);
    assertEquals(Rank.GENUS, oPlant.rank);

    LinneanClassification cl = new NameUsageMatch();
    cl.setKingdom("Plantae");
    cl.setPhylum("Tracheophyta");
    cl.setClazz("Magnoliopsida");
    cl.setOrder("Apiales");
    cl.setFamily("Apiaceae");
    // this is not the CoL source id, cause the CoL for tests only has families and above!
    assertClassification(nub, oPlant, "Apiaceae", cl);


    Usage oAnimal = nub.findUsages("Oenanthe Vieillot, 1816").get(0);
    assertEquals(Rank.GENUS, oAnimal.rank);

    cl = new NameUsageMatch();
    cl.setKingdom("Animalia");
    cl.setPhylum("Chordata");
    cl.setClazz("Aves");
    cl.setOrder("Passeriformes");
    cl.setFamily("Muscicapidae");
    // this is not the CoL source id, cause the CoL for tests only has families and above!
    assertClassification(nub, oAnimal, "Muscicapidae", cl);


    assertTrue(oPlant.key != oAnimal.key);
    assertTrue(oPlant.parentKey != oAnimal.parentKey);
    assertTrue(oPlant.nameKey != oAnimal.nameKey);

  }

  private void assertUsageDoesNotExists(ChecklistCache cache, String name) {
    List<Usage> usages = cache.findUsages(name);
    assertEquals("No usage should exist for " + name, 0, usages.size());
  }

  /**
   * @param sourceID check that usage.sourceKey is this one, -1 is ignored in comparisons
   */
  private Usage assertUsageExistsOnce(ChecklistCache cache, String name, Rank rank, TaxonomicStatus status,
    int sourceID) {
    List<Usage> usages = cache.findUsages(name);
    assertEquals("Exactly one usage should exist for " + name, 1, usages.size());
    Usage u = usages.get(0);
    assertEquals("Wrong rank for " + name, rank, u.rank);
    assertEquals("Wrong status for " + name, status, u.status);
    //System.out.println("Source id for " + name + " is " + u.sourceKey);
    if (sourceID != -1) {
      assertEquals("Wrong source id for " + name, sourceID, u.sourceKey);
    }

    return u;
  }

  private void assertClassification(ChecklistCache cache, Usage u, String parent, LinneanClassification cl) {
    final int usageKey = u.key;
    // check direct parent first
    Usage p = cache.get(u.parentKey);
    assertEquals("Wrong direct parent", parent, CacheUtils.nameOf(p, cache));

    // verify hierarchy
    if (!u.rank.isSpeciesOrBelow()) {
      assertFalse("Additional higher rank for " + usageKey + " found: " + cl.getHigherRank(u.rank),
        cl.getHigherRank(u.rank) == null);
      assertEquals(u.rank + " classification wrong", cl.getHigherRank(u.rank), CacheUtils.canonicalNameOf(u, cache));
      ClassificationUtils.setHigherRank(cl, u.rank, null);
    }
    while (u.hasParent()) {
      u = cache.get(u.parentKey);
      if (!u.rank.isSpeciesOrBelow()) {
        assertFalse("Additional higher rank for " + usageKey + " found: " + cl.getHigherRank(u.rank),
          cl.getHigherRank(u.rank) == null);
        assertEquals(u.rank + " classification wrong", cl.getHigherRank(u.rank), CacheUtils.canonicalNameOf(u, cache));
        ClassificationUtils.setHigherRank(cl, u.rank, null);
      }
    }

    assertFalse("Additional higher ranks expected: " + ClassificationUtils.getHigherClassification(cl),
      ClassificationUtils.hasContent(cl));
  }
}
