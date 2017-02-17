package org.gbif.checklistbank.service.mybatis.mapper;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VernacularNameMapperWithDataIT extends MapperITBase<VernacularNameMapper> {

  public VernacularNameMapperWithDataIT() {
    super(VernacularNameMapper.class, ClbDbTestRule.squirrels());
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

    mapper.processDataset(ClbDbTestRule.SQUIRRELS_DATASET_KEY, proc);
    assertEquals(6, proc.counter);
  }

}