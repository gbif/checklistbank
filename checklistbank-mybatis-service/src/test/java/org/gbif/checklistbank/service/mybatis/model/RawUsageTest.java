package org.gbif.checklistbank.service.mybatis.model;

import org.gbif.checklistbank.model.RawUsage;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

public class RawUsageTest {

  @Test
  public void testEquals() throws Exception {
    final int key = 100000001;
    final String json = "xeghwax542tgld@";
    final UUID dkey = UUID.randomUUID();
    final Date now = new Date();

    RawUsage raw = new RawUsage();
    raw.setUsageKey(key);
    raw.setDatasetKey(dkey);
    raw.setLastCrawled(now);
    raw.setJson(json);

    RawUsage raw2 = new RawUsage();
    raw2.setUsageKey(key);
    raw2.setDatasetKey(dkey);
    raw2.setLastCrawled(now);
    raw2.setJson(json);

    Assert.assertEquals(raw, raw2);

    raw.setLastCrawled(null);
    Assert.assertNotEquals(raw, raw2);

    raw2.setLastCrawled(null);
    Assert.assertEquals(raw, raw2);
  }
}
