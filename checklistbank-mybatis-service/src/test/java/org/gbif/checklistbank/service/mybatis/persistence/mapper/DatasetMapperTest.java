package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.checklistbank.model.DatasetCore;

import java.util.UUID;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DatasetMapperTest extends MapperITBase {

  private final DatasetMapper mapper;

  @Autowired
  public DatasetMapperTest(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      DataSource dataSource) {
    super(
        parsedNameMapper,
        nameUsageMapper,
        nubRelMapper,
        datasetMapper,
        citationMapper,
        dataSource,
        false);
    this.mapper = datasetMapper;
  }

  @Test
  public void truncateInsertUpdateGet() {
    final DatasetCore d = new DatasetCore();
    final UUID key = UUID.randomUUID();
    d.setKey(key);
    d.setPublisher(UUID.randomUUID());
    d.setParent(UUID.randomUUID());
    d.setTitle("Quadrophenia");

    final DatasetCore d2 = new DatasetCore();
    d2.setKey(key);
    d2.setTitle("Quadrophenia reloaded");
    d2.setPublisher(d.getPublisher());
    d2.setParent(d.getParent());

    mapper.truncate();
    mapper.insert(d);
    assertEquals(d, mapper.get(key));

    mapper.update(d2);
    assertEquals(d2, mapper.get(key));
    assertEquals(d2.getTitle(), mapper.get(key).getTitle());

    mapper.truncate();
    assertNull(mapper.get(key));

    mapper.insert(d);
    assertEquals(d, mapper.get(key));
    mapper.delete(key);
    assertNull(mapper.get(key));
  }
}
