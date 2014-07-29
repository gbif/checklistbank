package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingConstants;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NameUsageServiceMyBatisIT {

  private static final UUID CHECKLIST_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");
  private final int NOT_FOUND_KEY = -10;

  @Rule
  public DatabaseDrivenChecklistBankTestRule<NameUsageService> ddt =
    new DatabaseDrivenChecklistBankTestRule<NameUsageService>(NameUsageService.class);

  @Test
  public void testGet() {
    final NameUsage rodentia = ddt.getService().get(100000004, Locale.UK);
    final NameUsageMetrics rodentiaM = ddt.getService().getMetrics(100000004);
    assertNotNull(rodentia);
    assertEquals((Integer) 10, rodentia.getNubKey());
    assertFalse(rodentia.isSynonym());
    assertEquals("1000", rodentia.getTaxonID());
    assertEquals("Rodentia", rodentia.getCanonicalName());
    assertEquals("Rodentia Bowdich, 1821", rodentia.getScientificName());
    assertEquals("Bowdich, 1821", rodentia.getAuthorship());
    assertEquals(Rank.ORDER, rodentia.getRank());
    assertEquals((Integer) 100000003, rodentia.getParentKey());

    assertEquals("Animalia", rodentia.getKingdom());
    assertEquals((Integer) 100000001, rodentia.getKingdomKey());

    assertEquals("Chordata", rodentia.getPhylum());
    assertEquals((Integer) 100000002, rodentia.getPhylumKey());
    assertEquals(0, rodentiaM.getNumPhylum());

    assertEquals("Mammalia", rodentia.getClazz());
    assertEquals((Integer) 100000003, rodentia.getClassKey());
    assertEquals(0, rodentiaM.getNumClass());

    assertEquals(1, rodentiaM.getNumOrder());
    assertEquals(1, rodentiaM.getNumFamily());
    assertEquals(2, rodentiaM.getNumGenus());
    assertEquals(3, rodentiaM.getNumSpecies());
    assertEquals(8, rodentiaM.getNumSynonyms());
    assertEquals(1, rodentiaM.getNumChildren());
    assertEquals(24, rodentia.getNumDescendants());

    assertEquals(Origin.SOURCE, rodentia.getOrigin());

    assertEquals(UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4"), rodentia.getDatasetKey());
    assertNull(rodentia.getPublishedIn());

    assertEquals("1000", rodentia.getTaxonID());


    NameUsage squirrel = ddt.getService().get(100000025, Locale.UK);
    final NameUsageMetrics squirrelM = ddt.getService().getMetrics(100000025);
    assertNotNull(squirrel);
    assertNull(squirrel.getNubKey());
    assertFalse(squirrel.isSynonym());
    assertEquals("Sciurus vulgaris", squirrel.getCanonicalName());
    assertEquals("Sciurus vulgaris Linnaeus, 1758", squirrel.getScientificName());
    assertEquals("Linnaeus, 1758", squirrel.getAuthorship());
    assertEquals("Eurasian Red Squirrel", squirrel.getVernacularName());
    assertEquals(Rank.SPECIES, squirrel.getRank());
    assertEquals((Integer) 100000024, squirrel.getParentKey());

    assertEquals("Animalia", squirrel.getKingdom());
    assertEquals((Integer) 100000001, squirrel.getKingdomKey());

    assertEquals("Chordata", squirrel.getPhylum());
    assertEquals((Integer) 100000002, squirrel.getPhylumKey());
    assertEquals(0, squirrelM.getNumPhylum());

    assertEquals("Mammalia", squirrel.getClazz());
    assertEquals((Integer) 100000003, squirrel.getClassKey());
    assertEquals(0, squirrelM.getNumClass());

    assertEquals("Rodentia", squirrel.getOrder());
    assertEquals((Integer) 100000004, squirrel.getOrderKey());
    assertEquals(0, squirrelM.getNumOrder());

    assertEquals("Sciuridae", squirrel.getFamily());
    assertEquals((Integer) 100000005, squirrel.getFamilyKey());
    assertEquals(0, squirrelM.getNumFamily());

    assertEquals("Sciurus", squirrel.getGenus());
    assertEquals((Integer) 100000011, squirrel.getGenusKey());
    assertEquals(0, squirrelM.getNumGenus());

    assertEquals(1, squirrelM.getNumSpecies());
    assertEquals(9, squirrelM.getNumChildren());
    assertEquals(4, squirrelM.getNumSynonyms());

    assertEquals(UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4"), squirrel.getDatasetKey());
    assertEquals("Syst. Nat. , 10th ed. vol. 1 p. 63", squirrel.getPublishedIn());

    assertEquals("2010030", squirrel.getTaxonID());


    // TEST VERNACULAR
    squirrel = ddt.getService().get(100000040, null);
    assertNull(squirrel.getVernacularName());

    squirrel = ddt.getService().get(100000040, Locale.GERMANY);
    assertEquals("Kaukasischen Eichhörnchen", squirrel.getVernacularName());

    // TEST non existing language VERNACULAR
    squirrel = ddt.getService().get(100000040, Locale.CHINESE);
    assertEquals("Caucasian Squirrel", squirrel.getVernacularName());

    // TEST MULTIPLE IDENTIFIERS
    squirrel = ddt.getService().get(100000007, Locale.GERMANY);
    assertEquals("6905528", squirrel.getTaxonID());
    assertEquals(URI.create("http://www.catalogueoflife.org/details/species/id/6905528"), squirrel.getReferences());

    // TEST SYNONYM
    NameUsage syn = ddt.getService().get(100000027, Locale.FRENCH);
    assertNotNull(syn);
    assertTrue(syn.isSynonym());
    assertEquals("Sciurus nadymensis", syn.getCanonicalName());
    assertEquals("Sciurus nadymensis Serebrennikov, 1928", syn.getScientificName());
    assertNull(syn.getVernacularName());
    assertEquals(Rank.SPECIES, syn.getRank());
    assertEquals((Integer) 100000024, syn.getParentKey());
    assertEquals((Integer) 100000025, syn.getAcceptedKey());
    assertEquals("Sciurus vulgaris Linnaeus, 1758", syn.getAccepted());
    assertFalse(syn.isProParte());

  }

  @Test
  public void testArraySet() {
    // test nomenclatoral status set
    NameUsage syn = ddt.getService().get(100000026, Locale.FRENCH);
    //TODO: get the array mapper working
    assertEquals(2, syn.getNomenclaturalStatus().size());
  }

  @Test
  public void testGetNotFound() {
    assertNull(ddt.getService().get(NOT_FOUND_KEY, Locale.UK));
  }

  @Test
  public void testGetParsedName() {
    final ParsedName rodentia = ddt.getService().getParsedName(100000004);
    assertNotNull(rodentia);
    assertEquals("Rodentia", rodentia.getGenusOrAbove());
    assertEquals("Bowdich", rodentia.getAuthorship());
    assertEquals("1821", rodentia.getYear());
    assertNull(rodentia.getRank());
  }

  @Test
  public void testGetParsedNameNotFound() {
    assertNull(ddt.getService().getParsedName(NOT_FOUND_KEY));
  }

  @Test
  public void testList() {
    List<NameUsage> usages = ddt.getService().list(Locale.UK, null, null, null).getResults();
    assertEquals(PagingConstants.DEFAULT_PARAM_LIMIT, usages.size());

    // test paging
    Pageable page = new PagingRequest(1l, 1);
    usages = ddt.getService().list(Locale.UK, null, null, page).getResults();
    assertEquals(1, usages.size());
    NameUsage u1 = usages.get(0);

    page = new PagingRequest(0l, 2);
    usages = ddt.getService().list(Locale.UK, null, null, page).getResults();
    assertEquals(2, usages.size());
    assertEquals(u1, usages.get(1));

    // test by source id
    usages = ddt.getService().list(Locale.UK, null, "1", null).getResults();
    assertEquals(1, usages.size());
    assertEquals((Integer) 100000001, usages.get(0).getKey());

    // test by checklist key
    usages = ddt.getService().list(Locale.UK, UUID.fromString("d7dddbf4-2cf0-4f39-9b2a-bb099caae36c"), null, null)
      .getResults();
    assertEquals(2, usages.size());

    // test combined
    usages =
      ddt.getService().list(Locale.UK, UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4"), "1", null).getResults();
    assertEquals(1, usages.size());
    assertEquals((Integer) 100000001, usages.get(0).getKey());
  }

  @Test
  public void testByTaxonId() {
    List<NameUsage> usages = ddt.getService().list(Locale.UK, CHECKLIST_KEY, "100000", null).getResults();
    assertEquals(1, usages.size());

    assertEquals(ddt.getService().get(100000006, Locale.UK), usages.get(0));
  }

  @Test
  public void testListRelated() {
    List<NameUsage> usages = ddt.getService().listRelated(1, Locale.UK);
    assertEquals(1, usages.size());

    usages = ddt.getService().listRelated(1, Locale.UK, UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4"));
    assertEquals(1, usages.size());

    usages = ddt.getService().listRelated(1, Locale.UK, UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4"),
      UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f5"));
    assertEquals(1, usages.size());

    usages = ddt.getService().listRelated(1, Locale.UK, UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088ff"));
    assertEquals(0, usages.size());
  }

  @Test
  public void testListRelatedNotFound() {
    assertTrue(ddt.getService().listRelated(NOT_FOUND_KEY, Locale.UK).isEmpty());
  }

  @Test
  public void testListChildren() {
    List<NameUsage> usages = ddt.getService().listChildren(100000025, Locale.UK, null).getResults();
    assertEquals(9, usages.size());

    // assert we start with highest ranks, then lower ones
    assertEquals(Rank.SUBSPECIES, usages.get(0).getRank());
    assertEquals(Rank.SUBSPECIES, usages.get(1).getRank());
    assertEquals(Rank.VARIETY, usages.get(7).getRank());
    assertEquals(Rank.VARIETY, usages.get(8).getRank());

    // test paging
    Pageable page = new PagingRequest(3, 2);
    usages = ddt.getService().listChildren(100000025, Locale.UK, page).getResults();
    assertEquals(2, usages.size());

    page = new PagingRequest(2, 5);
    List<NameUsage> usages2 = ddt.getService().listChildren(100000025, Locale.UK, page).getResults();
    assertEquals(5, usages2.size());
    assertEquals(usages.get(0), usages2.get(1));
    assertEquals(usages.get(1), usages2.get(2));
  }

  @Test
  public void testListChildrenNotFound() {
    assertTrue(ddt.getService().listChildren(NOT_FOUND_KEY, Locale.UK, null).getResults().isEmpty());
  }

  @Test
  public void testListRoot() {
    List<NameUsage> usages = ddt.getService().listRoot(CHECKLIST_KEY, Locale.UK, null).getResults();
    assertEquals(1, usages.size());

    // test paging
    Pageable page = new PagingRequest(1l, 1);
    usages = ddt.getService().listRoot(CHECKLIST_KEY, Locale.UK, page).getResults();
    assertEquals(0, usages.size());

    page = new PagingRequest(0l, 2);
    usages = ddt.getService().listRoot(CHECKLIST_KEY, Locale.UK, page).getResults();
    assertEquals(1, usages.size());
  }

  @Test
  public void testListSynonyms() {
    List<NameUsage> usages = ddt.getService().listSynonyms(100000025, Locale.UK, null).getResults();
    assertEquals(4, usages.size());

    // test paging
    Pageable page = new PagingRequest(0, 2);
    usages = ddt.getService().listSynonyms(100000025, Locale.UK, page).getResults();
    assertEquals(2, usages.size());

    page = new PagingRequest(1, 2);
    List<NameUsage> usages2 = ddt.getService().listSynonyms(100000025, Locale.UK, page).getResults();
    assertEquals(2, usages2.size());
    assertEquals(usages.get(1), usages2.get(0));
  }

  @Test
  public void testListSynonymsNotFound() {
    assertTrue(ddt.getService().listSynonyms(NOT_FOUND_KEY, Locale.UK, null).getResults().isEmpty());
  }

  @Test
  public void testVerbatim() {
    // even though the record exists the verbatim smile data is empty, so null here
    assertNull(ddt.getService().getVerbatim(100000011));
    assertNull(ddt.getService().getVerbatim(NOT_FOUND_KEY));
  }

  @Test
  public void testUsageMetrics() {
    NameUsageMetrics m = ddt.getService().getMetrics(100000011);
    assertNotNull(m);
    assertEquals((Integer) 100000011, m.getKey());
    assertEquals(2, m.getNumChildren());
    assertEquals(12, m.getNumSynonyms());
    assertEquals(1, m.getNumGenus());
    assertEquals(2, m.getNumSubgenus());
    assertEquals(2, m.getNumSpecies());
    // not set in dbunit file
    assertEquals(0, m.getNumDescendants());
    assertEquals(0, m.getNumFamily());

    assertNull(ddt.getService().getMetrics(NOT_FOUND_KEY));
  }

}
