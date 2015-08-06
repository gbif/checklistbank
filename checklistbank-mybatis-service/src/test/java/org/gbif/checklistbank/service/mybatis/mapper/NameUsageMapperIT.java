package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.model.NameUsageWritable;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NameUsageMapperIT extends MapperITBase<NameUsageMapper> {

    private static final UUID DATASET_KEY = UUID.randomUUID();

    private ParsedNameMapper parsedNameMapper;

    public NameUsageMapperIT() {
        super(NameUsageMapper.class);
    }

    @Before
    public void initNameMapper() throws Exception {
        parsedNameMapper = getInstance(ParsedNameMapper.class);
    }

    private int createName(String name) {
        ParsedName pn = new ParsedName();
        pn.setType(NameType.WELLFORMED);
        pn.setScientificName(name);
        parsedNameMapper.create(pn, name);
        return pn.getKey();
    }

    private void deleteName(String name) {
        ParsedName pn = parsedNameMapper.getByName(name);
        if (pn != null) {
            parsedNameMapper.delete(pn.getKey());
        }
    }

    /**
     * Check all enum values have a matching postgres type value.
     */
    @Test
    public void testEnums() {
        String name = "Abies alba Mill.";
        deleteName(name);
        int nameKey = createName(name);

        NameUsageWritable u = new NameUsageWritable();
        u.setDatasetKey(DATASET_KEY);
        u.setNameKey(nameKey);
        for (Rank r : Rank.values()) {
            u.setKey(null);
            u.setRank(r);
            mapper.insert(u);
        }
        for (Origin o : Origin.values()) {
            u.setKey(null);
            u.setOrigin(o);
            mapper.insert(u);
        }
        for (TaxonomicStatus s : TaxonomicStatus.values()) {
            u.setKey(null);
            u.setTaxonomicStatus(s);
            mapper.insert(u);
        }
        for (NomenclaturalStatus s : NomenclaturalStatus.values()) {
            u.setKey(null);
            u.getNomenclaturalStatus().add(s);
            mapper.insert(u);
        }
        for (NameUsageIssue s : NameUsageIssue.values()) {
            u.setKey(null);
            u.getIssues().add(s);
            mapper.insert(u);
        }
    }

    /**
     * Check all enum values have a matching postgres type value.
     */
    @Test
    public void testListUsageRange() {
        List<NameUsage> list = mapper.listRange(0, 1000);
    }

    /**
     * Check all enum values have a matching postgres type value.
     */
    @Test
    public void testInsertWithKey() {
        String name = "Abies negra Mill.";
        deleteName(name);
        int nameKey = createName(name);

        NameUsageWritable u = new NameUsageWritable();
        u.setDatasetKey(DATASET_KEY);
        u.setNameKey(nameKey);
        u.setRank(Rank.SPECIES);
        mapper.insert(u);
        assertEquals(100000000, (int) u.getKey());

        u.setKey(110);
        mapper.insert(u);
        assertEquals(110, (int)u.getKey());

        u.setKey(null);
        mapper.insert(u);
        assertEquals(100000001, (int)u.getKey());
    }
}