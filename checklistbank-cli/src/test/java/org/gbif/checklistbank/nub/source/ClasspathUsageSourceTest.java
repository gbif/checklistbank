package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClasspathUsageSourceTest {

  /**
   * integration test with prod registry
   * @throws Exception
   */
  @Test
  public void testListSources() throws Exception {
    ClasspathUsageSource src = ClasspathUsageSource.allSources();
    List<NubSource> sources = src.listSources();
    assertEquals(16, sources.size());
    assertEquals(1, sources.get(0).priority);
    assertEquals(Rank.FAMILY, sources.get(0).ignoreRanksAbove);
  }

  @Test
  public void testIterateSource() throws Exception {
    ClasspathUsageSource src = ClasspathUsageSource.emptySource();
    NubSource fungi = new NubSource();
    fungi.name = "squirrels";
    fungi.key = UUID.fromString("d7dddbf4-2cf0-4f39-9b2a-99b0e2c3aa01");
    fungi.ignoreRanksAbove = Rank.SPECIES;
    int counter = 0;
    for (SrcUsage u : src.iterateSource(fungi)) {
      counter++;
      System.out.print(u.key + "  ");
      System.out.println(u.scientificName);
    }
    assertEquals(9, counter);
  }
}