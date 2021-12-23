package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.api.vocabulary.Language;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class VernacularNameServiceMyBatisIT extends MyBatisServiceITBase<VernacularNameService> {

    public VernacularNameServiceMyBatisIT() {
        super(VernacularNameService.class);
    }

    @Test
    public void testListByChecklistUsage() {
        // TEST VERNACULAR
        List<VernacularName> squirrels = service.listByUsage(100000025, null).getResults();
        assertEquals(2, squirrels.size());
        assertEquals("Eurasian Red Squirrel", squirrels.get(0).getVernacularName());
        assertEquals(Language.ENGLISH, squirrels.get(0).getLanguage());
        assertEquals("Europäisches Eichhörnchen", squirrels.get(1).getVernacularName());
        assertEquals(Language.GERMAN, squirrels.get(1).getLanguage());
        // TEST PAGING
        Pageable page = new PagingRequest(0, 1);
        VernacularName v1 = service.listByUsage(100000025, page).getResults().get(0);

        page = new PagingRequest(1, 1);
        VernacularName v2 = service.listByUsage(100000025, page).getResults().get(0);
        assertEquals(v1, squirrels.get(0));
        assertEquals(v2, squirrels.get(1));
    }

    @Test
    public void testListByRange() {
        // TEST VERNACULAR
        Map<Integer, List<VernacularName>> records = ((VernacularNameServiceMyBatis) service).listRange(1, 100000025);
        assertEquals(3, records.size());

        List<VernacularName> vernacularNames = records.get(100000025);
        assertEquals(2, vernacularNames.size());

        for (VernacularName v : vernacularNames) {
            assertNull(v.getSourceTaxonKey());
            assertNotNull(v.getVernacularName());
        }
    }

}
