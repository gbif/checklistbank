package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Reference;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DatasetImportServiceMyBatisTest {

    @Test
    public void testBuildCitation() throws Exception {
        Reference r = new Reference();
        assertNull(DatasetImportServiceMyBatis.buildCitation(r));

        r.setTitle("Fruitflies of Europe");
        assertEquals("Fruitflies of Europe", DatasetImportServiceMyBatis.buildCitation(r));

        r.setAuthor("Simmons, R.C");
        assertEquals("Simmons, R.C: Fruitflies of Europe", DatasetImportServiceMyBatis.buildCitation(r));

        r.setDate("1982");
        assertEquals("Simmons, R.C (1982) Fruitflies of Europe", DatasetImportServiceMyBatis.buildCitation(r));

        r.setSource("Animalia minor Vol.34");
        assertEquals("Simmons, R.C (1982) Fruitflies of Europe: Animalia minor Vol.34", DatasetImportServiceMyBatis.buildCitation(r));
    }
}