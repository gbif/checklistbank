package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.Rank;

import java.util.List;

import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import static org.junit.Assert.assertEquals;

public class ClasspathSourceListTest {

    /**
     * integration test with prod registry
     */
    @Test
    public void testListSources() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(1, 2, 3, 4, 5, 10, 11, 12, 23, 31);
        src.setSourceRank(23, Rank.KINGDOM);
        List<NubSource> sources = Iterables.toList(src);
        assertEquals(10, sources.size());
        assertEquals(1, sources.get(0).priority);
        assertEquals(Rank.FAMILY, sources.get(0).ignoreRanksAbove);
        assertEquals(23, sources.get(8).priority);
        assertEquals(Rank.KINGDOM, sources.get(8).ignoreRanksAbove);
    }

}