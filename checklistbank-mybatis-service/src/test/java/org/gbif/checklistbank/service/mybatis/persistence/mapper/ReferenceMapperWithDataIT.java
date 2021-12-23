package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.Reference;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule2;

import java.util.UUID;
import javax.sql.DataSource;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReferenceMapperWithDataIT extends MapperITBase {

  private final ReferenceMapper mapper;

  @Autowired
  public ReferenceMapperWithDataIT(
      ParsedNameMapper parsedNameMapper,
      NameUsageMapper nameUsageMapper,
      NubRelMapper nubRelMapper,
      DatasetMapper datasetMapper,
      CitationMapper citationMapper,
      ReferenceMapper referenceMapper,
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
    this.mapper = referenceMapper;
  }

  class NonEmptyCounter implements ResultHandler<Reference> {
    public int counter;

    @Override
    public void handleResult(ResultContext<? extends Reference> resultContext) {
      if (resultContext.getResultObject() != null) {
        counter++;
        Reference d = resultContext.getResultObject();
        System.out.println(d);
        assertNotNull(d.getTaxonKey());
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

    mapper.processDataset(ClbDbTestRule.SQUIRRELS_DATASET_KEY, proc);
    assertEquals(22, proc.counter);
  }
}
