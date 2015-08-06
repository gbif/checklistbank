package org.gbif.checklistbank.service.mybatis;

import org.gbif.checklistbank.service.CitationService;
import org.gbif.utils.text.StringUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CitationServiceMyBatisIT extends MyBatisServiceITBase<CitationService> {

    public CitationServiceMyBatisIT() {
        super(CitationService.class);
    }

    @Test
    public void testLargeCitations() throws Exception {
        String citation = StringUtils.randomString(100000);
        final Integer cid = service.createOrGet(citation);
        assertNotNull(cid);

        final Integer cid2 = service.createOrGet(citation);
        assertEquals(cid2, cid);
    }
}