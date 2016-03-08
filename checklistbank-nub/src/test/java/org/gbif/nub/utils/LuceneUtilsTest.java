package org.gbif.nub.utils;

import org.gbif.utils.text.StringUtils;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class LuceneUtilsTest {
  String test = "ŠŒŽšœžŸ¥µÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýÿ";

  @Test
  public void testAnalyze() throws Exception {

    AsciiKeywordAnalyzer ana = new AsciiKeywordAnalyzer();
    assertEquals("Herbert", LuceneUtils.analyze(ana, "Herbert").get(0));
    assertEquals("Cem Andrexi", LuceneUtils.analyze(ana, "Çem Ándrexï").get(0));

    assertEquals("SOEZsoezY¥µAAAAAAAECEEEEIIIIDNOOOOOOUUUUYssaaaaaaaeceeeeiiiidnoooooouuuuyy", LuceneUtils.analyze(ana, test).get(0));
    assertEquals(StringUtils.foldToAscii(test), LuceneUtils.analyze(ana, test).get(0));

    Stopwatch sw = Stopwatch.createStarted();
    for (int i=0; i<10000; i++) {
      String x = LuceneUtils.analyze(ana, "Çem Ándrexï").get(0);
      //String x = StringUtils.foldToAscii("Çem Ándrexï");
    }
    System.out.println("took " + sw.elapsed(TimeUnit.MILLISECONDS) + "ms");
  }
}