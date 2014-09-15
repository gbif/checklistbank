package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.vocabulary.CitesAppendix;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.LifeStage;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.OccurrenceStatus;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.api.vocabulary.ThreatStatus;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;
import org.gbif.dwc.terms.DwcTerm;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatasetImportServiceMyBatisIT {
  private static final UUID CHECKLIST_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");

  @Rule
  public DatabaseDrivenChecklistBankTestRule<DatasetImportService> ddt =
    new DatabaseDrivenChecklistBankTestRule<DatasetImportService>(DatasetImportService.class);


  @Test
  public void testDeleteDataset() throws Exception {
    int num = ddt.getService().deleteDataset(CHECKLIST_KEY);
    assertEquals(44, num);
  }

  @Test
  public void testDeleteOldUsages() throws Exception {
    int num = ddt.getService().deleteOldUsages(CHECKLIST_KEY, new Date(10000l));
    assertEquals(0, num);

    num = ddt.getService().deleteOldUsages(CHECKLIST_KEY, new Date());
    assertEquals(44, num);
  }

  @Test
  public void testSyncUsage() throws Exception {
    final int key = 7896;
    final String taxonID = "gfzd8";
    String name = "Abies alba Mill.";

    NameUsageContainer u = new NameUsageContainer();
    u.setKey(key);
    u.setScientificName(name);
    u.setTaxonID(taxonID);
    u.setOrigin(Origin.SOURCE);
    u.setSynonym(false);
    u.getIssues().add(NameUsageIssue.MULTIMEDIA_INVALID);
    u.getIssues().add(NameUsageIssue.CHAINED_SYNOYM);
    u.setLastCrawled(new Date());
    u.setLastInterpreted(new Date());
    u.setModified(new Date());
    u.getNomenclaturalStatus().add(NomenclaturalStatus.CONSERVED);
    u.getNomenclaturalStatus().add(NomenclaturalStatus.DOUBTFUL);
    u.setRank(Rank.SUBSPECIES);
    u.setRemarks("neat neat neat");
    u.setTaxonomicStatus(TaxonomicStatus.HETEROTYPIC_SYNONYM);
    u.setSourceTaxonKey(674321);
    u.setReferences(URI.create("http://www.gbif.org/1234"));
    u.setNumDescendants(321);

    u.getDescriptions().add(buildDescription());
    u.getDistributions().add(buildDistribution());
    u.getIdentifiers().add(buildIdentifier());
    u.getSpeciesProfiles().add(buildSpeciesProfile());
    u.getVernacularNames().add(buildVernacularName());

    NameUsageMetrics m = new NameUsageMetrics();
    m.setKey(key);
    m.setNumSpecies(1);
    m.setNumDescendants(4);
    m.setNumSynonyms(3);

    ddt.getService().syncUsage(CHECKLIST_KEY, u, null, m);

    // with verbatim data
    VerbatimNameUsage v = new VerbatimNameUsage();
    v.setKey(key);
    v.setCoreField(DwcTerm.scientificName, name);
    v.setCoreField(DwcTerm.taxonID, taxonID);

    ddt.getService().syncUsage(CHECKLIST_KEY, u, v, m);
  }

  private Description buildDescription() {
    Description d = new Description();
    d.setLicense("my licsnse");
    d.setLanguage(Language.ABKHAZIAN);
    d.setSource("mz source");
    d.setCreator("markus");
    d.setType("headline");
    d.setDescription("nothing to read");
    return d;
  }

  private Distribution buildDistribution() {
    Distribution d = new Distribution();
    d.setSource("my source");
    d.setLifeStage(LifeStage.ADULT);
    d.setThreatStatus(ThreatStatus.CRITICALLY_ENDANGERED);
    d.setStatus(OccurrenceStatus.ABSENT);
    d.setTemporal("temp");
    d.setStartDayOfYear(12);
    d.setEndDayOfYear(23);
    d.setLocationId("tdwg:ARG");
    d.setLocality("Somewhere in Argentinia");
    d.setAppendixCites(CitesAppendix.II);
    d.setCountry(Country.ARGENTINA);
    d.setSourceTaxonKey(123);
    return d;
  }

  private Identifier buildIdentifier() {
    Identifier i = new Identifier();
    return i;
  }

  private NameUsageMediaObject buildMedia() {
    NameUsageMediaObject i = new NameUsageMediaObject();
    return i;
  }

  private SpeciesProfile buildSpeciesProfile() {
    SpeciesProfile i = new SpeciesProfile();
    return i;
  }

  private VernacularName buildVernacularName() {
    VernacularName i = new VernacularName();
    return i;
  }

  }