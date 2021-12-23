package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.*;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.LinneanClassificationKeys;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.*;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.service.UsageSyncService;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.nameparser.NameParserGbifV1;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** */
public class UsageSyncServiceMyBatisIT extends MyBatisServiceITBase2 {

  private final UsageSyncService service;
  private final NameUsageService nameUsageService;

  private static final NameParser PARSER = new NameParserGbifV1();

  @Autowired
  public UsageSyncServiceMyBatisIT(
      DataSource dataSource, UsageSyncService usageSyncService, NameUsageService nameUsageService) {
    super(dataSource);
    this.service = usageSyncService;
    this.nameUsageService = nameUsageService;
  }

  @Test
  public void testDeleteDataset() throws Exception {
    int num = service.deleteDataset(ClbDbTestRule.SQUIRRELS_DATASET_KEY);
    assertEquals(44, num);
  }

  @Test
  public void nubRelations() throws Exception {
    Map<Integer, Integer> rels = new HashMap<>();
    rels.put(1, 1);
    service.insertNubRelations(ClbDbTestRule.SQUIRRELS_DATASET_KEY, rels);
  }

  @Test
  public void testSyncUsage() throws Exception {

    // first add a classification to please constraints
    NameUsage k = addHigher(1, null, null, "Plantae", Rank.KINGDOM);
    NameUsage p = addHigher(2, k.getKey(), k, "Pinophyta", Rank.PHYLUM);
    NameUsage c = addHigher(3, p.getKey(), p, "Pinopsida", Rank.CLASS);
    NameUsage o = addHigher(4, c.getKey(), c, "Pinales", Rank.ORDER);
    NameUsage f = addHigher(5, o.getKey(), o, "Pinaceae", Rank.FAMILY);
    NameUsage g = addHigher(6, f.getKey(), f, "Abies", Rank.GENUS);

    // create rather complete usage to sync WITHOUT a KEY
    final String taxonID = "gfzd8";
    String name = "Abies alba Mill.";

    NameUsage u = new NameUsage();
    u.setDatasetKey(ClbDbTestRule.SQUIRRELS_DATASET_KEY);
    u.setScientificName(name);
    u.setTaxonID(taxonID);
    u.setOrigin(Origin.SOURCE);
    u.getIssues().add(NameUsageIssue.MULTIMEDIA_INVALID);
    u.getIssues().add(NameUsageIssue.CHAINED_SYNOYM);
    u.setModified(new Date());
    u.getNomenclaturalStatus().add(NomenclaturalStatus.CONSERVED);
    u.getNomenclaturalStatus().add(NomenclaturalStatus.DOUBTFUL);
    u.setRank(Rank.SPECIES);
    u.setRemarks("neat neat neat");
    u.setTaxonomicStatus(TaxonomicStatus.DOUBTFUL);
    u.setSourceTaxonKey(674321);
    u.setReferences(URI.create("http://www.gbif.org/1234"));
    u.setAccordingTo("Chuck told me this");
    u.setNumDescendants(321);

    u.setNubKey(p.getKey());

    UsageExtensions e = new UsageExtensions();
    e.descriptions.add(buildDescription());
    e.distributions.add(buildDistribution());
    e.identifiers.add(buildIdentifier());
    e.speciesProfiles.add(buildSpeciesProfile());
    e.vernacularNames.add(buildVernacularName());

    NameUsageMetrics m = new NameUsageMetrics();
    m.setNumSpecies(1);
    m.setNumDescendants(4);
    m.setNumSynonyms(3);
    u.setParentKey(g.getKey());
    u.setBasionymKey(-1); // point to itself!
    u.setProParteKey(-1); // point to itself!
    u.setKingdomKey(k.getKey());
    u.setPhylumKey(p.getKey());
    u.setClassKey(c.getKey());
    u.setOrderKey(o.getKey());
    u.setFamilyKey(f.getKey());
    u.setGenusKey(g.getKey());
    u.setSpeciesKey(u.getKey());

    int k1 = service.syncUsage(false, u, PARSER.parse(u.getScientificName(), u.getRank()), m);

    // verify props
    NameUsage u2 = nameUsageService.get(k1, null);
    assertEquals(k.getKey(), u2.getKingdomKey());
    assertEquals(p.getKey(), u2.getPhylumKey());
    assertEquals(c.getKey(), u2.getClassKey());
    assertEquals(o.getKey(), u2.getOrderKey());
    assertEquals(f.getKey(), u2.getFamilyKey());
    assertEquals(g.getKey(), u2.getGenusKey());
    assertEquals(g.getKey(), u2.getParentKey());
    assertEquals((Integer) k1, u2.getBasionymKey());
    assertEquals((Integer) k1, u2.getProParteKey());

    assertEquals(u.getTaxonID(), u2.getTaxonID());
    assertEquals(u.getScientificName(), u2.getScientificName());
    assertEquals(u.getIssues(), u2.getIssues());
    assertEquals(ClbDbTestRule.SQUIRRELS_DATASET_KEY, u2.getDatasetKey());
    assertEquals(u.getTaxonomicStatus(), u2.getTaxonomicStatus());
    assertEquals(u.getAccordingTo(), u2.getAccordingTo());
    assertEquals(u.getRemarks(), u2.getRemarks());
    assertEquals(u.getOrigin(), u2.getOrigin());
    assertNotNull(u2.getLastInterpreted());
    assertEquals(u.getModified(), u2.getModified());
    assertEquals(p.getKey(), u2.getNubKey());
    assertEquals(u.getSourceTaxonKey(), u2.getSourceTaxonKey());

    // Try an update now with verbatim data (remove usage key as we detect existing record by
    // taxonID only!)
    u.setKey(null);
    u.setBasionymKey(-1); // point to itself!
    u.setProParteKey(-1); // point to itself!
    m.setKey(null);
    VerbatimNameUsage v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.scientificName, name);
    v.setCoreField(DwcTerm.taxonID, taxonID);

    int k2 = service.syncUsage(false, u, PARSER.parse(u.getScientificName(), u.getRank()), m);
    service.syncUsageExtras(false, ClbDbTestRule.SQUIRRELS_DATASET_KEY, u.getKey(), v, e);
    assertEquals(k1, k2);

    // verify props
    u2 = nameUsageService.get(k1, null);
    assertEquals(k.getKey(), u2.getKingdomKey());
    assertEquals(p.getKey(), u2.getPhylumKey());
    assertEquals(c.getKey(), u2.getClassKey());
    assertEquals(o.getKey(), u2.getOrderKey());
    assertEquals(f.getKey(), u2.getFamilyKey());
    assertEquals(g.getKey(), u2.getGenusKey());
    assertEquals(g.getKey(), u2.getParentKey());
    assertEquals((Integer) k1, u2.getBasionymKey());
    assertEquals((Integer) k1, u2.getProParteKey());
    assertEquals(p.getKey(), u2.getNubKey());
  }

  /** Makes sure all db enums are matching the API enum values */
  @Test
  public void testAllEnums() throws Exception {
    String name = "Abies mekka Jesus";

    NameUsage u = new NameUsage();
    u.setDatasetKey(ClbDbTestRule.SQUIRRELS_DATASET_KEY);
    u.setScientificName(name);
    u.setOrigin(Origin.SOURCE);
    u.setModified(new Date());
    // all enums
    u.getIssues().addAll(Sets.newHashSet(NameUsageIssue.values()));
    u.getNomenclaturalStatus().addAll(Sets.newHashSet(NomenclaturalStatus.values()));
    u.setRank(Rank.SPECIES);
    u.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);

    NameUsageMetrics m = new NameUsageMetrics();

    ParsedName pn = PARSER.parse(u.getScientificName(), u.getRank());

    final int uID = service.syncUsage(false, u, pn, m);
    List<Integer> ids = Lists.newArrayList(uID);

    for (Rank r : Rank.values()) {
      m.setKey(null);
      u.setKey(null);
      u.setRank(r);
      // there are far more ranks than status values
      if (r.ordinal() < TaxonomicStatus.values().length) {
        u.setTaxonomicStatus(TaxonomicStatus.values()[r.ordinal()]);
      }
      ids.add(service.syncUsage(true, u, pn, m));
    }

    NameUsage u2 = nameUsageService.get(uID, null);
    u2.setLastInterpreted(null);
    u2.setNameKey(null);
    u2.setCanonicalName(null);
    u2.setNameType(null);
    u2.setAuthorship(null);

    u.setKey(uID);
    u.setRank(Rank.SPECIES);
    u.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);

    assertEquals(u, u2);
  }

  private NameUsage addHigher(
      int key, Integer parentKey, LinneanClassificationKeys higherKeys, String name, Rank rank)
      throws UnparsableException {
    NameUsage p = new NameUsage();
    p.setDatasetKey(ClbDbTestRule.SQUIRRELS_DATASET_KEY);
    p.setKey(key);
    p.setParentKey(parentKey);
    if (higherKeys != null) {
      ClassificationUtils.copyLinneanClassificationKeys(higherKeys, p);
    }
    // add link to oneself
    if (rank.isLinnean()) {
      ClassificationUtils.setHigherRankKey(p, rank, key);
    }
    p.setScientificName(name);
    p.setRank(rank);
    p.setTaxonID(name);
    p.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);
    p.setOrigin(Origin.SOURCE);
    p.setLastInterpreted(new Date());
    p.setModified(new Date());

    NameUsageMetrics m = new NameUsageMetrics();
    m.setKey(key);

    service.syncUsage(false, p, PARSER.parse(p.getScientificName(), p.getRank()), m);

    return p;
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
    d.setStatus(DistributionStatus.ABSENT);
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
