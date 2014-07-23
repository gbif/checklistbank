package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.vocabulary.Origin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NormalizerStatsTest {

  @Test
  public void testSetRecords() throws Exception {
    NormalizerStats stats = new NormalizerStats();
    stats.setRecords(221);
    assertEquals(221, stats.getRecords());
    assertEquals(221, stats.getUsages());
    assertEquals(221, stats.getCountByOrigin(Origin.SOURCE));

    stats.setRecords(5);
    assertEquals(5, stats.getRecords());
    assertEquals(5, stats.getUsages());
    assertEquals(5, stats.getCountByOrigin(Origin.SOURCE));
  }
}