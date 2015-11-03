package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.LifeStage;
import org.gbif.api.vocabulary.Sex;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VernacularNameMapperTest extends NameUsageComponentMapperITBase<VernacularNameMapper> {

    public VernacularNameMapperTest() {
        super(VernacularNameMapper.class);
    }

    @Test
    public void testMapper() throws Exception {
        assertNull(mapper.getByChecklistUsage(usageKey, Language.ENGLISH.getIso2LetterCode()));
        assertNull(mapper.getByNubUsage(usageKey, Language.ENGLISH.getIso2LetterCode()));
        assertTrue(mapper.listByChecklistUsage(usageKey, new PagingRequest()).isEmpty());
        assertTrue(mapper.listByNubUsage(usageKey, new PagingRequest()).isEmpty());

        VernacularName v = new VernacularName();
        v.setCountry(Country.AFGHANISTAN);
        v.setArea("kundus");
        v.setLanguage(Language.AFAR);
        v.setLifeStage(LifeStage.EMRYO);
        v.setPlural(false);
        v.setPreferred(true);
        v.setSex(Sex.FEMALE);
        v.setVernacularName("Afghanische Gans");
        // these should get ignored
        v.setSource("sourcy s");
        v.setSourceTaxonKey(123);

        mapper.insert(usageKey, v, citationKey1);

        VernacularName v2 = mapper.getByChecklistUsage(usageKey, Language.AFAR.getIso2LetterCode());
        assertEquals(v.getVernacularName(), v2.getVernacularName());
        assertEquals(v.getLanguage(), v2.getLanguage());
        assertEquals(v.getSex(), v2.getSex());
        assertEquals(v.getLifeStage(), v2.getLifeStage());
        assertEquals(v.isPlural(), v2.isPlural());
        assertEquals(v.isPreferred(), v2.isPreferred());
        assertEquals(v.getArea(), v2.getArea());
        assertEquals(v.getCountry(), v2.getCountry());
        // these are handled special
        assertEquals(citation1, v2.getSource());
        assertNull(v2.getSourceTaxonKey());

        VernacularName v3 = mapper.getByNubUsage(nubKey, Language.AFAR.getIso2LetterCode());
        assertEquals(v.getVernacularName(), v3.getVernacularName());
        assertEquals(v.getLanguage(), v3.getLanguage());
        assertEquals(v.getSex(), v3.getSex());
        assertEquals(v.getLifeStage(), v3.getLifeStage());
        assertEquals(v.isPlural(), v3.isPlural());
        assertEquals(v.isPreferred(), v3.isPreferred());
        assertEquals(v.getArea(), v3.getArea());
        assertEquals(v.getCountry(), v3.getCountry());
        // these are now nub source usage values
        assertEquals(datasetTitle, v3.getSource());
        assertEquals((Integer)usageKey, v3.getSourceTaxonKey());
    }
}