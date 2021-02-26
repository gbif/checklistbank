package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.nub.NeoTmpRepoRule;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RandomSourceTest {

  @Rule
  public NeoTmpRepoRule neoRepo = new NeoTmpRepoRule();

  @Test
  public void testUsages() throws Exception {
    RandomSource src = new RandomSource(100, Kingdom.ANIMALIA, neoRepo.cfg);
    src.init(false, false);

    int counter = 0;
    try (CloseableIterator<SrcUsage> iter = src.iterator()) {
      while (iter.hasNext()) {
        SrcUsage u = iter.next();
        counter++;
        System.out.print(u.key + "  ");
        System.out.print(u.rank + "  ");
        System.out.println(u.scientificName);
      }
    }
    assertEquals(100, counter);
  }
}