package org.gbif.nub.utils;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class LuceneUtilsTest {

  @Test
  public void testAnalyze() throws Exception {

    AsciiKeywordAnalyzer ana = new AsciiKeywordAnalyzer();
    assertEquals("Herbert", LuceneUtils.analyze(ana, "Herbert").get(0));
    assertEquals("Cem Andrexi", LuceneUtils.analyze(ana, "Çem Ándrexï").get(0));

    Stopwatch sw = Stopwatch.createStarted();
    for (int i=0; i<100000; i++) {
      String x = LuceneUtils.analyze(ana, "Çem Ándrexï").get(0);
      //String x = StringNormalizer.foldToAscii("Çem Ándrexï");
    }
    System.out.println("took " + sw.elapsed(TimeUnit.MILLISECONDS) + "ms");
  }
}