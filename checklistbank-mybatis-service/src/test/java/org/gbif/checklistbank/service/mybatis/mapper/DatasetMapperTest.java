package org.gbif.checklistbank.service.mybatis.mapper;

import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DatasetMapperTest extends MapperITBase<DatasetMapper> {

    public DatasetMapperTest() {
        super(DatasetMapper.class, false);
    }

    @Test
    public void truncateInsertGet() {
        final String title = "Quadrophenia";

        final UUID key = UUID.randomUUID();
        mapper.truncate();
        mapper.insert(key, title);
        assertEquals(title, mapper.get(key));

        mapper.truncate();
        assertNull(mapper.get(key));
    }
}