package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.model.RawUsage;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class NubRelMapperTest extends MapperITBase<NubRelMapper> {

    public NubRelMapperTest() {
        super(NubRelMapper.class, true);
    }

    @Test
    public void process() {
        AtomicInteger counter = new AtomicInteger();
        mapper.process(Constants.NUB_DATASET_KEY).forEach(u -> {
            counter.getAndIncrement();
        });
        assertEquals(0, counter.get());
    }
}