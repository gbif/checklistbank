package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.utils.text.StringUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ParsedNameMapperIT extends MapperITBase<ParsedNameMapper> {

    public ParsedNameMapperIT() {
        super(ParsedNameMapper.class, false);
    }

    /**
     * Check all enum values have a matching postgres type value.
     */
    @Test
    public void testEnums() {
        ParsedName pn = new ParsedName();
        for (NameType p : NameType.values()) {
            pn.setKey(null);
            pn.setScientificName(StringUtils.randomSpecies());
            pn.setType(p);
            mapper.create(pn, "canonical");
        }
        for (Rank r : Rank.values()) {
            pn.setKey(null);
            pn.setScientificName(StringUtils.randomSpecies());
            pn.setRank(r);
            mapper.create(pn, "canonical");
        }
        for (NamePart p : NamePart.values()) {
            pn.setKey(null);
            pn.setScientificName(StringUtils.randomSpecies());
            pn.setNotho(p);
            mapper.create(pn, "canonical");
        }
    }

    /**
     * Check all enum values have a matching postgres type value.
     */
    @Test
    public void testGetMax() {
        assertNull(mapper.maxKey());
        ParsedName pn = new ParsedName();
        pn.setType(NameType.CULTIVAR);
        for (int x = 0; x < 10; x++) {
            pn.setKey(null);
            pn.setScientificName(StringUtils.randomSpecies());
            mapper.create(pn, "canonical");
        }
        assertEquals((Integer) 10, mapper.maxKey());
    }
}