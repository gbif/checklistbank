package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.IdentifierService;
import org.gbif.api.vocabulary.IdentifierType;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IdentifierServiceMyBatisIT extends MyBatisServiceITBase<IdentifierService> {

    public IdentifierServiceMyBatisIT() {
        super(IdentifierService.class);
    }

    private final Integer USAGE_ID = 100000007;

    @Test
    public void testListByUsage() {
        List<Identifier> ids = service.listByUsage(USAGE_ID, null).getResults();
        assertEquals(3, ids.size());

        Set<IdentifierType> types = Sets.newHashSet(IdentifierType.DOI, IdentifierType.URL, IdentifierType.LSID);
        for (Identifier id : ids) {
            switch (id.getType()) {
                case LSID:
                    assertEquals("urn:lsid:catalogueoflife.org:taxon:df0a319c-29c1-102b-9a4a-00304854f820:col20120721",
                            id.getIdentifier());
                    assertNull(id.getTitle());
                    break;
                case DOI:
                    assertEquals("doi:10.1038/6905528", id.getIdentifier());
                    assertNull(id.getTitle());
                    break;
                case URL:
                    assertEquals("http://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=632417",
                            id.getIdentifier());
                    assertNull(id.getTitle());
                    break;
                default:
                    fail("Unkown Identifier");
            }
            types.remove(id.getType());
        }
        assertTrue(types.isEmpty());
        // TEST PAGING
        Pageable page = new PagingRequest(0, 1);
        Identifier d1 = service.listByUsage(USAGE_ID, page).getResults().get(0);

        page = new PagingRequest(1, 1);
        Identifier d2 = service.listByUsage(USAGE_ID, page).getResults().get(0);
        assertEquals(d1, ids.get(0));
        assertEquals(d2, ids.get(1));
    }

}