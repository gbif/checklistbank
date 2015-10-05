package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.utils.ResourcesMonitor;

import java.util.List;

import com.beust.jcommander.internal.Lists;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.helpers.collection.Iterables;

import static org.junit.Assert.assertEquals;

public class ClasspathSourceListTest {

    @Test
    public void testListSources() throws Exception {
        ClasspathSourceList src = ClasspathSourceList.source(1, 2, 3, 4, 5, 10, 11, 23, 12, 31);
        src.setSourceRank(23, Rank.KINGDOM);
        List<NubSource> sources = Iterables.toList(src);
        assertEquals(10, sources.size());
        assertEquals(Rank.FAMILY, sources.get(0).ignoreRanksAbove);
        assertEquals(Rank.KINGDOM, sources.get(7).ignoreRanksAbove);
    }


    /**
     * Test large amount of nub sources to see if neo4j / dao resources are managed properly
     */
    @Test
    @Ignore("Manual test to debug too many open files or other neo4j resource problems")
    public void testLargeLists() throws Exception {
        ResourcesMonitor monitor = new ResourcesMonitor();
        monitor.run();

        List<ClasspathSource> sources = Lists.newArrayList();
        // try 30 classpath sources 100 times = 3000 sources!
        for (int rep=0; rep<100; rep++) {
            for (int id=1; id<31; id++) {
                sources.add(new ClasspathSource(id));
            }
        }
        ClasspathSourceList srcList = new ClasspathSourceList(sources);
        monitor.run();
        for (NubSource src : srcList) {
            int counter = 0;
            for (SrcUsage u : src) {
                counter++;
            }
            src.close();
            monitor.run();
        }
    }

}