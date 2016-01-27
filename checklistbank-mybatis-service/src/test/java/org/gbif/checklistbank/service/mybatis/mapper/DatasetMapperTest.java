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
  public void truncateInsertUpdateGet() {
    final String title = "Quadrophenia";
    final String title2 = "Quadrophenia reloaded";

    final UUID key = UUID.randomUUID();
    mapper.truncate();
    mapper.insert(key, title);
    assertEquals(title, mapper.get(key));

    mapper.update(key, title2);
    assertEquals(title2, mapper.get(key));

    mapper.truncate();
    assertNull(mapper.get(key));

    mapper.insert(key, title);
    assertEquals(title, mapper.get(key));
    mapper.delete(key);
    assertNull(mapper.get(key));
  }
}