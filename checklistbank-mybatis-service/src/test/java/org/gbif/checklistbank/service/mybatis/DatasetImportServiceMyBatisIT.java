package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.LinneanClassificationKeys;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.api.util.ClassificationUtils;
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
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.dwc.terms.DwcTerm;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DatasetImportServiceMyBatisIT extends MyBatisServiceITBase<DatasetImportService> {

    public DatasetImportServiceMyBatisIT() {
        super(DatasetImportService.class);
    }

    private static final UUID CHECKLIST_KEY = UUID.fromString("109aea14-c252-4a85-96e2-f5f4d5d088f4");

    @Test
    public void testDeleteDataset() throws Exception {
        int num = service.deleteDataset(CHECKLIST_KEY);
        assertEquals(44, num);
    }

    @Test
    public void testSyncUsage() throws Exception {
        final NameUsageService uService = getInstance(NameUsageService.class);

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
        u.setDatasetKey(CHECKLIST_KEY);
        u.setScientificName(name);
        u.setTaxonID(taxonID);
        u.setOrigin(Origin.SOURCE);
        u.setSynonym(false);
        u.getIssues().add(NameUsageIssue.MULTIMEDIA_INVALID);
        u.getIssues().add(NameUsageIssue.CHAINED_SYNOYM);
        u.setModified(new Date());
        u.getNomenclaturalStatus().add(NomenclaturalStatus.CONSERVED);
        u.getNomenclaturalStatus().add(NomenclaturalStatus.DOUBTFUL);
        u.setRank(Rank.SUBSPECIES);
        u.setRemarks("neat neat neat");
        u.setTaxonomicStatus(TaxonomicStatus.HETEROTYPIC_SYNONYM);
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

        int k1 = service.syncUsage(false, u, null, m, e);

        // verify props
        NameUsage u2 = uService.get(k1, null);
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
        assertEquals(CHECKLIST_KEY, u2.getDatasetKey());
        assertEquals(u.getTaxonomicStatus(), u2.getTaxonomicStatus());
        assertEquals(u.getAccordingTo(), u2.getAccordingTo());
        assertEquals(u.getRemarks(), u2.getRemarks());
        assertEquals(u.getOrigin(), u2.getOrigin());
        assertNotNull(u2.getLastInterpreted());
        assertEquals(u.getModified(), u2.getModified());
        assertEquals(p.getKey(), u2.getNubKey());
        assertEquals(u.getSourceTaxonKey(), u2.getSourceTaxonKey());

        // Try an update now with verbatim data (remove usage key as we detect existing record by taxonID only!)
        u.setKey(null);
        u.setBasionymKey(-1); // point to itself!
        u.setProParteKey(-1); // point to itself!
        m.setKey(null);
        VerbatimNameUsage v = new VerbatimNameUsage();
        v.setCoreField(DwcTerm.scientificName, name);
        v.setCoreField(DwcTerm.taxonID, taxonID);

        int k2 = service.syncUsage(false, u, v, m, e);
        assertEquals(k1, k2);

        // verify props
        u2 = uService.get(k1, null);
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


    private NameUsage addHigher(int key, Integer parentKey, LinneanClassificationKeys higherKeys, String name, Rank rank) {
        NameUsage p = new NameUsage();
        p.setDatasetKey(CHECKLIST_KEY);
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
        p.setSynonym(false);
        p.setOrigin(Origin.SOURCE);
        p.setLastInterpreted(new Date());
        p.setModified(new Date());

        NameUsageMetrics m = new NameUsageMetrics();
        m.setKey(key);

        service.syncUsage(false, p, null, m, null);

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