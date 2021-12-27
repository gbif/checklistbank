package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule2;

import java.util.UUID;
import javax.sql.DataSource;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class VernacularNameMapperWithDataIT extends MapperITBase {

  private final VernacularNameMapper mapper;

  @Autowired
  public VernacularNameMapperWithDataIT(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      VernacularNameMapper vernacularNameMapper,
      DataSource dataSource) {
    super(
        parsedNameMapper,
        nameUsageMapper,
        nubRelMapper,
        datasetMapper,
        citationMapper,
        dataSource,
        false,
        ClbDbTestRule2.squirrels(dataSource));
    this.mapper = vernacularNameMapper;
  }

  class NonEmptyCounter implements ResultHandler<VernacularName> {
    public int counter;

    @Override
    public void handleResult(ResultContext<? extends VernacularName> resultContext) {
      if (resultContext.getResultObject() != null) {
        counter++;
        VernacularName d = resultContext.getResultObject();
        System.out.println(d);
        assertNotNull(d.getTaxonKey());
        assertNotNull(d.getVernacularName());
      }
    }
  }

  @Test
  public void testProcessDataset() {
    NonEmptyCounter proc = new NonEmptyCounter();
    mapper.processDataset(UUID.randomUUID(), proc);
    assertEquals(0, proc.counter);

    mapper.processDataset(Constants.NUB_DATASET_KEY, proc);
    assertEquals(0, proc.counter);

    mapper.processDataset(ClbDbTestRule2.SQUIRRELS_DATASET_KEY, proc);
    assertEquals(6, proc.counter);
  }
}
