package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.vocabulary.CitesAppendix;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.EstablishmentMeans;
import org.gbif.api.vocabulary.LifeStage;
import org.gbif.api.vocabulary.OccurrenceStatus;
import org.gbif.api.vocabulary.ThreatStatus;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DistributionMapperTest extends MapperITBase<DistributionMapper> {

    public DistributionMapperTest() {
        super(DistributionMapper.class, true);
    }

    @Test
    public void testMapper() throws Exception {
        assertTrue(mapper.listByChecklistUsage(usageKey, new PagingRequest()).isEmpty());
        assertTrue(mapper.listByNubUsage(usageKey, new PagingRequest()).isEmpty());

        Distribution obj = new Distribution();
        obj.setAppendixCites(CitesAppendix.II);
        obj.setCountry(null); //global
        obj.setEndDayOfYear(1890);
        obj.setEstablishmentMeans(EstablishmentMeans.NATIVE);
        obj.setLifeStage(LifeStage.EMRYO);
        obj.setLocality("location location location");
        obj.setLocationId("locID");
        obj.setRemarks("remarks");
        obj.setStartDayOfYear(1889);
        obj.setStatus(OccurrenceStatus.COMMON);
        obj.setTemporal("aha");
        obj.setThreatStatus(ThreatStatus.CRITICALLY_ENDANGERED);
        // these should get ignored
        obj.setSource("sourcy s");
        obj.setSourceTaxonKey(123);
        mapper.insert(usageKey, obj, citationKey1);

        obj = new Distribution();
        obj.setAppendixCites(CitesAppendix.II);
        obj.setCountry(Country.ALGERIA);
        obj.setEndDayOfYear(1990);
        obj.setEstablishmentMeans(EstablishmentMeans.NATIVE);
        obj.setLifeStage(LifeStage.EMRYO);
        obj.setLocality("location location location");
        obj.setLocationId("locID");
        obj.setRemarks("remarks");
        obj.setStartDayOfYear(1989);
        obj.setStatus(OccurrenceStatus.COMMON);
        obj.setTemporal("aha");
        obj.setThreatStatus(ThreatStatus.CRITICALLY_ENDANGERED);
        // these should get ignored
        obj.setSource("sourcy s");
        obj.setSourceTaxonKey(123);
        mapper.insert(usageKey, obj, citationKey1);

        List<Distribution> list = mapper.listByChecklistUsage(usageKey, new PagingRequest());
        assertEquals(2, list.size());
        assertNull(list.get(0).getCountry());
        assertObject(obj, list.get(1), citation1, null);

        list = mapper.listByNubUsage(nubKey, new PagingRequest());
        assertEquals(2, list.size());
        // these are now nub source usage values
        assertNull(list.get(0).getCountry());
        assertObject(obj, list.get(1), datasetTitle, usageKey);
    }

    private void assertObject(Distribution obj, Distribution obj2, String source, Integer sourceTaxonKey) {
        assertEquals(obj.getAppendixCites(), obj2.getAppendixCites());
        assertEquals(obj.getCountry(), obj2.getCountry());
        assertEquals(obj.getEndDayOfYear(), obj2.getEndDayOfYear());
        assertEquals(obj.getEstablishmentMeans(), obj2.getEstablishmentMeans());
        assertEquals(obj.getLifeStage(), obj2.getLifeStage());
        assertEquals(obj.getLocality(), obj2.getLocality());
        assertEquals(obj.getLocationId(), obj2.getLocationId());
        assertEquals(obj.getRemarks(), obj2.getRemarks());
        assertEquals(obj.getStartDayOfYear(), obj2.getStartDayOfYear());
        assertEquals(obj.getStatus(), obj2.getStatus());
        assertEquals(obj.getTemporal(), obj2.getTemporal());
        assertEquals(obj.getThreatStatus(), obj2.getThreatStatus());

        assertEquals(source, obj2.getSource());
        assertEquals(sourceTaxonKey, obj2.getSourceTaxonKey());
    }
}