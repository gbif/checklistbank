package org.gbif.checklistbank.neo;

import org.apache.commons.lang3.ArrayUtils;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.normalizer.NeoTest;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class NeoMapperTest extends NeoTest {

    @Test
    public void testPropertyMap() throws Exception {
        NeoMapper mapper = NeoMapper.instance();
        NameUsage u = usage();

        Map<String, Object> map = mapper.propertyMap(u, false);
        assertEquals(100, map.get("key"));
        assertEquals("Abies alba", map.get("scientificName"));
        assertEquals(20, map.get("basionymKey"));
        assertEquals(23, map.get("parentKey"));
        assertEquals("Abies", map.get("parent"));
        assertEquals(1, map.get("classKey"));
        assertEquals("Trees", map.get("clazz"));
        assertNotNull(map.get("lastCrawled"));
        assertNotNull(map.get("lastInterpreted"));
        assertEquals(Rank.SPECIES.ordinal(), map.get("rank"));
        assertEquals(TaxonomicStatus.ACCEPTED.ordinal(), map.get("taxonomicStatus"));
        assertEquals(NameType.SCINAME.ordinal(), map.get("nameType"));
        assertEquals(7, map.get("numDescendants"));

        assertEquals(2, ((int[]) map.get("nomenclaturalStatus")).length);
        assertTrue(ArrayUtils.contains( (int[]) map.get("nomenclaturalStatus"), NomenclaturalStatus.CONSERVED.ordinal()));
        assertTrue(ArrayUtils.contains( (int[]) map.get("nomenclaturalStatus"), NomenclaturalStatus.ILLEGITIMATE.ordinal()));

        // isSynonym is set to false by default
        assertEquals(15, map.size());
    }

    private NameUsage usage() {
        NameUsage u = new NameUsage();
        u.setKey(100);
        u.setScientificName("Abies alba");
        u.setBasionymKey(20);
        u.setParentKey(23);
        u.setParent("Abies");
        u.setClassKey(1);
        u.setClazz("Trees");
        u.setLastCrawled(new Date());
        u.setLastInterpreted(new Date());
        u.setRank(Rank.SPECIES);
        u.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);
        u.setNameType(NameType.SCINAME);
        u.setNumDescendants(7);
        u.getNomenclaturalStatus().add(NomenclaturalStatus.CONSERVED);
        u.getNomenclaturalStatus().add(NomenclaturalStatus.ILLEGITIMATE);
        return u;
    }

    @Test
    public void testNodeStore() throws Exception {
        NeoMapper mapper = NeoMapper.instance();
        initDb(UUID.randomUUID());

        try (Transaction tx = beginTx()) {
            NameUsage u = usage();
            Node n = db.createNode();

            mapper.store(n, u, true);
            assertEquals(100, n.getProperty("key"));
            assertEquals("Abies alba", n.getProperty("scientificName"));
            assertEquals(20, n.getProperty("basionymKey"));
            assertEquals(23, n.getProperty("parentKey"));
            assertEquals("Abies", n.getProperty("parent"));
            assertEquals(1, n.getProperty("classKey"));
            assertEquals("Trees", n.getProperty("clazz"));
            assertNotNull(n.getProperty("lastCrawled"));
            assertNotNull(n.getProperty("lastInterpreted"));
            assertEquals(Rank.SPECIES.ordinal(), n.getProperty("rank"));
            assertEquals(TaxonomicStatus.ACCEPTED.ordinal(), n.getProperty("taxonomicStatus"));
            assertEquals(NameType.SCINAME.ordinal(), n.getProperty("nameType"));
            assertEquals(7, n.getProperty("numDescendants"));

            assertEquals(2, ((int[]) n.getProperty("nomenclaturalStatus")).length);
            assertTrue(ArrayUtils.contains( (int[]) n.getProperty("nomenclaturalStatus"), NomenclaturalStatus.CONSERVED.ordinal()));
            assertTrue(ArrayUtils.contains( (int[]) n.getProperty("nomenclaturalStatus"), NomenclaturalStatus.ILLEGITIMATE.ordinal()));

            // isSynonym is set to false by default
            assertEquals(15, IteratorUtil.count(n.getPropertyKeys()));

        }
    }

    @Test
    public void testRoundtrip() throws Exception {
        NeoMapper mapper = NeoMapper.instance();
        initDb(UUID.randomUUID());

        try (Transaction tx = beginTx()) {
            NameUsage u1 = usage();
            Node n = db.createNode();

            mapper.store(n, u1, true);
            NameUsage u2 = mapper.read(n, new NameUsage());

            assertEquals(u1, u2);
        }
    }
}