package org.gbif.checklistbank.service.mybatis.mapper;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.checklistbank.model.ParsedNameUsage;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DistributionMapperWithDataIT extends MapperITBase<DistributionMapper> {

  public DistributionMapperWithDataIT() {
    super(DistributionMapper.class, ClbDbTestRule.squirrels());
  }

  class NonEmptyCounter implements ResultHandler<Distribution> {
    public int counter;

    @Override
    public void handleResult(ResultContext<? extends Distribution> resultContext) {
      if (resultContext.getResultObject() != null) {
        counter++;
        Distribution d = resultContext.getResultObject();
        System.out.println(d);
        assertNotNull(d.getTaxonKey());
        assertNotNull(d.getLocality());
      }
    }
  }

  @Test
  public void testProcessDataset() {
    NonEmptyCounter proc = new NonEmptyCounter();
    mapper.processDataset(UUID.randomUUID(), proc);
    assertEquals(0, proc.counter);

    mapper.processDataset(ClbDbTestRule.SQUIRRELS_DATASET_KEY, proc);
    assertEquals(14, proc.counter);

    mapper.processDataset(Constants.NUB_DATASET_KEY, proc);
    // we did not reset counter, so it adds up
    assertEquals(14, proc.counter);

  }

}