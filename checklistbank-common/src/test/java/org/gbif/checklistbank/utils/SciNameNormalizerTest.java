package org.gbif.checklistbank.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class SciNameNormalizerTest {

  @Test
  public void testNormalize() throws Exception {
    assertEquals(null, SciNameNormalizer.normalize(""));
    assertEquals("Abies", SciNameNormalizer.normalize("Abies "));
    assertEquals("Abies", SciNameNormalizer.normalize("Abiies "));
    assertEquals("Abies", SciNameNormalizer.normalize("Abyes "));
    assertEquals("Abies alba", SciNameNormalizer.normalize("Abyes  albus"));
    assertEquals("Abies alba", SciNameNormalizer.normalize(" \txAbies × ållbbus\t"));
  }

  @Test
  public void testEpithetStemming() throws Exception {
    assertEquals(null, SciNameNormalizer.stemEpithet(""));
    assertEquals("alb", SciNameNormalizer.stemEpithet("alba"));
    assertEquals("alb", SciNameNormalizer.stemEpithet("albus"));
    assertEquals("alb", SciNameNormalizer.stemEpithet("albon"));
    assertEquals("alb", SciNameNormalizer.stemEpithet("album"));

    assertEquals("allb", SciNameNormalizer.stemEpithet("allbus"));
    assertEquals("alab", SciNameNormalizer.stemEpithet("alaba"));
    assertEquals("ala", SciNameNormalizer.stemEpithet("alaus"));
    assertEquals("ala", SciNameNormalizer.stemEpithet("alaa"));
  }
}
