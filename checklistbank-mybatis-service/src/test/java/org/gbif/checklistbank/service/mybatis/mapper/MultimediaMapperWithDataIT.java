package org.gbif.checklistbank.service.mybatis.mapper;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MultimediaMapperWithDataIT extends MapperITBase<MultimediaMapper> {

  public MultimediaMapperWithDataIT() {
    super(MultimediaMapper.class, ClbDbTestRule.squirrels());
  }

  class NonEmptyCounter implements ResultHandler<NameUsageMediaObject> {
    public int counter;

    @Override
    public void handleResult(ResultContext<? extends NameUsageMediaObject> resultContext) {
      if (resultContext.getResultObject() != null) {
        counter++;
        NameUsageMediaObject d = resultContext.getResultObject();
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
    assertEquals(11, proc.counter);
  }

}