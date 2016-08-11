package org.gbif.checklistbank.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class SciNameNormalizerTest {

  @Test
  public void testNormalize() throws Exception {
    assertEquals(null, SciNameNormalizer.normalize(""));
    assertEquals("Abies", SciNameNormalizer.normalize("Abies "));
    assertEquals("Abies", SciNameNormalizer.normalize("Abiies "));
    assertEquals("Abyes", SciNameNormalizer.normalize("Abyes "));
    assertEquals("Abyes alba", SciNameNormalizer.normalize("Abyes  albus"));
    assertEquals("Abyes albieta", SciNameNormalizer.normalize("Abyes albieta"));
    assertEquals("Abies albieta", SciNameNormalizer.normalize("Abies albijeta"));
    assertEquals("Abies albieta", SciNameNormalizer.normalize("Abies albyeta"));
    assertEquals("Abies alba", SciNameNormalizer.normalize(" \txAbies × ållbbus\t"));

    assertEquals("Abies alba", SciNameNormalizer.normalize(" \txAbies × ållbbus\t"));
    assertEquals("Rhachis taktos", SciNameNormalizer.normalize("Rhachis taktos"));

    assertEquals("Hieracium sabauda", SciNameNormalizer.normalize("Hieracium sabaudum"));
    assertEquals("Hieracium scorzoneraefolia", SciNameNormalizer.normalize("Hieracium scorzoneræfolium"));
    assertEquals("Hieracium scorzonerifolia", SciNameNormalizer.normalize("Hieracium scorzonerifolium"));
    assertEquals("Macrozamia platirachis", SciNameNormalizer.normalize("Macrozamia platyrachis"));
    assertEquals("Macrozamia platirachis", SciNameNormalizer.normalize("Macrozamia platyrhachis"));
    assertEquals("Cycas circinalis", SciNameNormalizer.normalize("Cycas circinalis"));
    assertEquals("Cycas circinalis", SciNameNormalizer.normalize("Cycas circinnalis"));
    assertEquals("Isolona perieri", SciNameNormalizer.normalize("Isolona perrieri"));
    assertEquals("Isolona perieri", SciNameNormalizer.normalize("Isolona perrierii"));

    assertEquals("Carex caioueti", SciNameNormalizer.normalize("Carex ×cayouettei"));
    assertEquals("Platanus hispanica", SciNameNormalizer.normalize("Platanus x hispanica"));
    // https://github.com/gbif/checklistbank/issues/7
    assertEquals("Eragrostis browni", SciNameNormalizer.normalize("Eragrostis brownii"));
    assertEquals("Eragrostis browni", SciNameNormalizer.normalize("Eragrostis brownei"));
  }

  @Test
  public void testHybridCross() throws Exception {
    assertEquals("xcayouettei", SciNameNormalizer.removeHybridCross("xcayouettei"));
    assertEquals(" cayouettei", SciNameNormalizer.removeHybridCross("×cayouettei"));

    assertEquals("Carex xcayouettei", SciNameNormalizer.removeHybridCross("Carex xcayouettei"));
    assertEquals("Carex cayouettei", SciNameNormalizer.removeHybridCross("Carex ×cayouettei"));
    assertEquals("Carex cayouettei", SciNameNormalizer.removeHybridCross("×Carex cayouettei"));
    assertEquals("Carex xcayouettei", SciNameNormalizer.removeHybridCross("xCarex xcayouettei"));
    assertEquals("Carex cayouettei", SciNameNormalizer.removeHybridCross("XCarex cayouettei"));
    assertEquals("Carex cayouettei", SciNameNormalizer.removeHybridCross("xCarex ×cayouettei"));

    assertEquals("Platanus hispanica", SciNameNormalizer.removeHybridCross("Platanus x hispanica"));

  }

  @Test
  public void testNonAscii() throws Exception {
    assertEquals("Cem Andrexi", SciNameNormalizer.normalize("Çem Ándrexï"));
    assertEquals("SOEZsoezY¥µAECEIDNOUYsaeceidnouy", SciNameNormalizer.normalize("ŠŒŽšœžŸ¥µÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýÿ"));
  }

  @Test
  public void testEpithetStemming() throws Exception {
    assertEquals(null, SciNameNormalizer.stemEpithet(""));
    assertEquals("alba", SciNameNormalizer.stemEpithet("alba"));
    assertEquals("alba", SciNameNormalizer.stemEpithet("albus"));
    assertEquals("alba", SciNameNormalizer.stemEpithet("albon"));
    assertEquals("alba", SciNameNormalizer.stemEpithet("album"));

    assertEquals("allba", SciNameNormalizer.stemEpithet("allbus"));
    assertEquals("alaba", SciNameNormalizer.stemEpithet("alaba"));
    assertEquals("alaa", SciNameNormalizer.stemEpithet("alaus"));
    assertEquals("alaa", SciNameNormalizer.stemEpithet("alaa"));
  }
}
