package org.gbif.checklistbank.service.mybatis.persistence.mapper;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.model.ParsedNameUsage;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule2;

import java.util.UUID;
import javax.sql.DataSource;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NameUsageMapperWithDataIT extends MapperITBase {

  private final NameUsageMapper mapper;

  @Autowired
  public NameUsageMapperWithDataIT(
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
        false,
        ClbDbTestRule2.squirrels(dataSource));
    this.mapper = nameUsageMapper;
  }

  class NonEmptyCounter implements ResultHandler<ParsedNameUsage> {
    public int counter;

    @Override
    public void handleResult(ResultContext<? extends ParsedNameUsage> resultContext) {
      if (resultContext.getResultObject() != null) {
        counter++;
        ParsedNameUsage u = resultContext.getResultObject();
        assertNotNull(u.getKey());
        assertNotNull(u.getScientificName());
        assertNotNull(u.getRank());
        assertNotNull(u.getDatasetKey());

        assertNotNull(u.getParsedName());
        assertNotNull(u.getParsedName().getScientificName());
        System.out.println(u.getParsedName());
      }
    }
  }

  @Test
  public void testProcessDataset() {
    NonEmptyCounter proc = new NonEmptyCounter();
    mapper.processDataset(UUID.randomUUID(), proc);
    assertEquals(0, proc.counter);

    mapper.processDataset(Constants.NUB_DATASET_KEY, proc);
    assertEquals(2, proc.counter);

    mapper.processDataset(ClbDbTestRule2.SQUIRRELS_DATASET_KEY, proc);
    // we did not reset counter, so it adds up
    assertEquals(46, proc.counter);
  }
}
