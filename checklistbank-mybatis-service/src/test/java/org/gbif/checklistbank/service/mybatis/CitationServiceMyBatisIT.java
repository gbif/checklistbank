package org.gbif.checklistbank.service.mybatis;

import org.gbif.checklistbank.service.CitationService;
import org.gbif.checklistbank.service.mybatis.postgres.DatabaseDrivenChecklistBankTestRule;
import org.gbif.utils.text.StringUtils;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CitationServiceMyBatisIT {

    @Rule
    public DatabaseDrivenChecklistBankTestRule<CitationService> ddt =
            new DatabaseDrivenChecklistBankTestRule<CitationService>(CitationService.class);

    @Test
    public void testLargeCitations() throws Exception {
        String citation = StringUtils.randomString(100000);
        final Integer cid = ddt.getService().createOrGet(citation);
        assertNotNull(cid);

        final Integer cid2 = ddt.getService().createOrGet(citation);
        assertEquals(cid2, cid);
    }
}