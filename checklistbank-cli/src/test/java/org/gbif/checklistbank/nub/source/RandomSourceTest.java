package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.checklistbank.nub.model.SrcUsage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RandomSourceTest {

    @Test
    public void testUsages() throws Exception {
        RandomSource src = new RandomSource(100, Kingdom.ANIMALIA);
        src.init(false, false, false);

        int counter = 0;
        for (SrcUsage u : src) {
            counter++;
            System.out.print(u.key + "  ");
            System.out.print(u.rank + "  ");
            System.out.println(u.scientificName);
        }
        assertEquals(100, counter);
    }
}