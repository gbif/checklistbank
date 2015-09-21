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
    }

}