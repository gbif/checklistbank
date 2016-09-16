package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.model.NameUsageWritable;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class NameUsageMetricsMapperIT extends MapperITBase<NameUsageMetricsMapper> {

    private static final UUID DATASET_KEY = UUID.randomUUID();

    private ParsedNameMapper nameMapper;
    private NameUsageMapper usageMapper;

    public NameUsageMetricsMapperIT() {
        super(NameUsageMetricsMapper.class, false);
    }

    @Before
    public void setupOtherMappers() {
        nameMapper = getInstance(ParsedNameMapper.class);
        usageMapper = getInstance(NameUsageMapper.class);
    }

    private int createUsage(String name, Rank rank) {
        // name first
        ParsedName pn = nameMapper.getByName(name, rank);
        if (pn == null) {
            pn = new ParsedName();
            pn.setType(NameType.SCIENTIFIC);
            pn.setScientificName(name);
            nameMapper.create(pn);
        }
        // name usage
        NameUsageWritable nu = new NameUsageWritable();
        nu.setDatasetKey(DATASET_KEY);
        nu.setRank(Rank.SPECIES);
        nu.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);
        nu.setSynonym(false);
        nu.setNameKey(pn.getKey());
        nu.setNumDescendants(100);
        usageMapper.insert(nu);
        return nu.getKey();
    }

    /**
     * Check all enum values have a matching postgres type value.
     */
    @Test
    public void testInsertAndRead() {
        String name = "Abies alba Mill.";
        int usageKey = createUsage(name, Rank.SPECIES);

        NameUsageMetrics m = new NameUsageMetrics();
        m.setKey(usageKey);
        m.setNumChildren(1);
        m.setNumClass(2);
        m.setNumDescendants(3);
        m.setNumFamily(4);
        m.setNumGenus(5);
        m.setNumOrder(6);
        m.setNumPhylum(7);
        m.setNumSpecies(8);
        m.setNumSubgenus(9);
        m.setNumSynonyms(10);

        mapper.insert(DATASET_KEY, m);

        NameUsageMetrics m2 = mapper.get(usageKey);
        assertNotEquals(m2, m);

        // this should be ignored and taken from the name_usage instead on reads
        m.setNumDescendants(100);
        assertEquals(m2, m);
    }

}