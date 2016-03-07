package org.gbif.checklistbank.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class StringNormalizerTest {

  @Test
  public void testFoldToAscii() throws Exception {
    assertEquals(null, StringNormalizer.foldToAscii(null));
    assertEquals("", StringNormalizer.foldToAscii(""));
    assertEquals("Schulhof, Gymnasium Hurth", StringNormalizer.foldToAscii("Schulhof, Gymnasium Hürth"));
    assertEquals("Doring", StringNormalizer.foldToAscii("Döring"));
    assertEquals("Desireno", StringNormalizer.foldToAscii("Désírèñø"));
    assertEquals("Debreczy & I. Racz", StringNormalizer.foldToAscii("Debreçzÿ & Ï. Rácz"));
    assertEquals("Donatia novae-zelandiae", StringNormalizer.foldToAscii("Donatia novae-zelandiæ"));
    assertEquals("Carex ×cayouettei", StringNormalizer.foldToAscii("Carex ×cayouettei"));
    assertEquals("Carex comosa × Carex lupulina", StringNormalizer.foldToAscii("Carex comosa × Carex lupulina"));
    assertEquals("Aeropyrum coil-shaped virus", StringNormalizer.foldToAscii("Aeropyrum coil-shaped virus"));
    assertEquals("†Lachnus bonneti", StringNormalizer.foldToAscii("†Lachnus bonneti"));

  }
}