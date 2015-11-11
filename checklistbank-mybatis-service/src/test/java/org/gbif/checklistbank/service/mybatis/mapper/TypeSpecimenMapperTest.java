package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TypeDesignationType;
import org.gbif.api.vocabulary.TypeStatus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TypeSpecimenMapperTest extends MapperITBase<TypeSpecimenMapper> {

    public TypeSpecimenMapperTest() {
        super(TypeSpecimenMapper.class, true);
    }

    @Test
    public void testMapper() throws Exception {
        assertTrue(mapper.listByChecklistUsage(usageKey, new PagingRequest()).isEmpty());
        assertTrue(mapper.listByNubUsage(usageKey, new PagingRequest()).isEmpty());

        TypeSpecimen obj = new TypeSpecimen();
        obj.setScientificName("Abies alba");
        obj.setTypeDesignatedBy("Markus");
        obj.setTypeDesignationType(TypeDesignationType.ORIGINAL_DESIGNATION);
        obj.setTaxonRank(Rank.SPECIES);
        // these are legacy properties not stored in CLB - we only store type species/genus records, not specimens as these are occurrences!
        obj.setTypeStatus(TypeStatus.TYPE_SPECIES);
        obj.setCitation(citation2);
        obj.setLocality("locality");
        obj.setCatalogNumber("catNum177");
        // these should get ignored
        obj.setSource("sourcy s");
        obj.setSourceTaxonKey(123);

        mapper.insert(usageKey, obj, citationKey1);

        TypeSpecimen obj2 = mapper.listByChecklistUsage(usageKey, new PagingRequest()).get(0);
        assertEquals(obj.getScientificName(), obj2.getScientificName());
        assertEquals(obj.getTypeDesignatedBy(), obj2.getTypeDesignatedBy());
        assertEquals(obj.getTypeDesignationType(), obj2.getTypeDesignationType());
        assertEquals(obj.getTaxonRank(), obj2.getTaxonRank());
        // deprecated fields
        assertNull(obj2.getTypeStatus());
        assertNull(obj2.getCitation());
        assertNull(obj2.getLocality());
        assertNull(obj2.getCatalogNumber());
        assertNull(obj2.getRecordedBy());
        // these are handled special
        assertEquals(citation1, obj2.getSource());
        assertNull(obj2.getSourceTaxonKey());

        TypeSpecimen obj3 = mapper.listByNubUsage(nubKey, new PagingRequest()).get(0);
        assertEquals(obj.getScientificName(), obj3.getScientificName());
        assertEquals(obj.getTypeDesignatedBy(), obj3.getTypeDesignatedBy());
        assertEquals(obj.getTypeDesignationType(), obj3.getTypeDesignationType());
        assertEquals(obj.getTaxonRank(), obj3.getTaxonRank());
        // deprecated fields
        assertNull(obj3.getTypeStatus());
        assertNull(obj3.getCitation());
        assertNull(obj3.getLocality());
        assertNull(obj3.getCatalogNumber());
        assertNull(obj3.getRecordedBy());
        // these are now nub source usage values
        assertEquals(datasetTitle, obj3.getSource());
        assertEquals((Integer) usageKey, obj3.getSourceTaxonKey());
    }
}