package org.gbif.checklistbank.nub.source;

import org.gbif.checklistbank.iterable.CloseableIterator;
import org.gbif.checklistbank.nub.NeoTmpRepoRule;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by markus on 29/09/15.
 */
public class ClasspathSourceTest {

  @ClassRule
  public static NeoTmpRepoRule neoRepo = new NeoTmpRepoRule();

  @Test
  public void testUsages() throws Exception {
    ClasspathSource src = new ClasspathSource(1, neoRepo.cfg);
    src.init(false, false);

    int counter = 0;
    try (CloseableIterator<SrcUsage> iter = src.iterator()) {
      while (iter.hasNext()) {
        SrcUsage u = iter.next();
        counter++;
        System.out.print(u.key + "  ");
        System.out.print(u.scientificName + " :: ");
        System.out.println(u.publishedIn);
      }
    }
    assertEquals(12, counter);

  }
}