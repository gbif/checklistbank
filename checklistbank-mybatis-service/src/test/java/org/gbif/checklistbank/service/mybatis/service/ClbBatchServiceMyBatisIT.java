package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.service.UsageService;

import java.net.URI;
import java.util.List;

import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClbBatchServiceMyBatisIT extends MyBatisServiceITBase<UsageService> {

    public ClbBatchServiceMyBatisIT() {
        super(UsageService.class);
    }

    @Test
    public void testListAll() {
        List<Integer> ids = service.listAll();
        assertEquals(46, ids.size());
    }

    @Test
    public void testListRange() {
        List<NameUsage> usages = service.listRange(100000001, 100000020);
        assertEquals(20, usages.size());

        boolean found = false;
        for (NameUsage nu : usages) {
            assertNull(nu.getVernacularName());
            assertNotNull(nu.getNameKey());
            assertNotNull(nu.getScientificName());
            assertTrue(nu.getKey() >= 100000001 && nu.getKey() <= 100000020);
            assertEquals(ClbDbTestRule.SQUIRRELS_DATASET_KEY, nu.getDatasetKey());

            if (nu.getKey().equals(100000007)) {
                found = true;
                assertEquals("6905528", nu.getTaxonID());
                assertEquals(URI.create("http://www.catalogueoflife.org/details/species/id/6905528"), nu.getReferences());
            }
        }
        if (!found) {
            fail("usage 100000007 missing in range result");
        }
    }

}
