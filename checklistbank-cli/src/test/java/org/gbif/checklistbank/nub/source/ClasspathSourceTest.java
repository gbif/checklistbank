package org.gbif.checklistbank.nub.source;

import org.gbif.checklistbank.nub.model.SrcUsage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by markus on 29/09/15.
 */
public class ClasspathSourceTest {

    @Test
    public void testUsages() throws Exception {
        ClasspathSource src = new ClasspathSource(1);
        src.init(false, false, false);

        int counter = 0;
        for (SrcUsage u : src) {
            counter++;
            System.out.print(u.key + "  ");
            System.out.println(u.scientificName);
        }
        assertEquals(12, counter);

    }
}