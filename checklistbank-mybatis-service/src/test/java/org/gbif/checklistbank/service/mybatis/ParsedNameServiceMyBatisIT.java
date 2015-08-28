package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.service.ParsedNameService;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParsedNameServiceMyBatisIT extends MyBatisServiceITBase<ParsedNameService> {

    public ParsedNameServiceMyBatisIT() {
        super(ParsedNameService.class);
    }

    @Test
    public void testCreateOrGet() throws Exception {
        ParsedName pn = service.createOrGet("Abies alba Mill.", null);
        assertEquals("Abies alba Mill.", pn.getScientificName());
        assertEquals("Abies alba", pn.canonicalName());
        assertEquals("Abies", pn.getGenusOrAbove());
        assertEquals("alba", pn.getSpecificEpithet());
        assertEquals("Mill.", pn.getAuthorship());

        pn = service.createOrGet("Abies sp.", null);
        assertEquals("Abies sp.", pn.getScientificName());
        assertEquals("Abies spec.", pn.canonicalName());
        assertEquals("Abies", pn.getGenusOrAbove());
        assertEquals("sp.", pn.getRankMarker());
        assertEquals(Rank.SPECIES, pn.getRank());
        Assert.assertNull(pn.getSpecificEpithet());

        pn = service.createOrGet("×Abies Mill.", null);
        assertEquals("×Abies Mill.", pn.getScientificName());
        assertEquals("Abies", pn.canonicalName());
        assertEquals("Abies", pn.getGenusOrAbove());
        Assert.assertNull(pn.getRankMarker());
        Assert.assertNull(pn.getRank());
        Assert.assertNull(pn.getSpecificEpithet());
        assertEquals(NamePart.GENERIC, pn.getNotho());
    }

    @Test
    public void testReparse() throws Exception {
        assertEquals(1150, service.reparseAll());
    }

}