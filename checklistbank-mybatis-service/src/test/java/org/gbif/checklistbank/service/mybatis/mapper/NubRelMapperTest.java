package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.Constants;

import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/** */
public class NubRelMapperTest extends MapperITBase {

  private final NubRelMapper mapper;

  @Autowired
  public NubRelMapperTest(
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
        true);
    this.mapper = nubRelMapper;
  }

  @Test
  public void process() {
    AtomicInteger counter = new AtomicInteger();
    mapper
        .process(Constants.NUB_DATASET_KEY)
        .forEach(
            u -> {
              counter.getAndIncrement();
            });
    assertEquals(0, counter.get());
  }
}
