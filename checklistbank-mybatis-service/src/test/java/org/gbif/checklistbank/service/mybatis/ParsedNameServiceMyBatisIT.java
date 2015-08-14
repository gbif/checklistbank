package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.service.ParsedNameService;

import org.junit.Assert;
import org.junit.Test;

public class ParsedNameServiceMyBatisIT extends MyBatisServiceITBase<ParsedNameService> {

    public ParsedNameServiceMyBatisIT() {
        super(ParsedNameService.class);
    }

    @Test
    public void testCreateOrGet() throws Exception {
        ParsedName pn = service.createOrGet("Abies alba Mill.", null);
        Assert.assertEquals("Abies alba Mill.", pn.getScientificName());
        Assert.assertEquals("Abies alba", pn.canonicalName());
        Assert.assertEquals("Abies", pn.getGenusOrAbove());
        Assert.assertEquals("alba", pn.getSpecificEpithet());
        Assert.assertEquals("Mill.", pn.getAuthorship());

        pn = service.createOrGet("Abies sp.", null);
        Assert.assertEquals("Abies sp.", pn.getScientificName());
        Assert.assertEquals("Abies spec.", pn.canonicalName());
        Assert.assertEquals("Abies", pn.getGenusOrAbove());
        Assert.assertEquals("sp.", pn.getRankMarker());
        Assert.assertEquals(Rank.SPECIES, pn.getRank());
        Assert.assertNull(pn.getSpecificEpithet());

        pn = service.createOrGet("×Abies Mill.", null);
        Assert.assertEquals("×Abies Mill.", pn.getScientificName());
        Assert.assertEquals("Abies", pn.canonicalName());
        Assert.assertEquals("Abies", pn.getGenusOrAbove());
        Assert.assertNull(pn.getRankMarker());
        Assert.assertNull(pn.getRank());
        Assert.assertNull(pn.getSpecificEpithet());
        Assert.assertEquals(NamePart.GENERIC, pn.getNotho());
    }
}