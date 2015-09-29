package org.gbif.checklistbank.nub.source;

import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.iterable.CloseableIterable;
import org.gbif.checklistbank.iterable.FutureIterator;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.collect.Lists;


/**
 * UsageSource implementation that works on classpath files for testing nub builds.
 * This class is just a test helper class, see NubBuilderTest for its use.
 * For every dataset source there needs to be simple flat usage tab file under resources/nub-sources
 * with the following columns:
 * This abstract class reads a tab delimited text stream expected with the following columns:
 * <ul>
 * <li>usageKey</li>
 * <li>parentKey</li>
 * <li>basionymKey</li>
 * <li>rank (enum)</li>
 * <li>isSynonym (f/t)</li>
 * <li>taxonomicStatus (enum)</li>
 * <li>nomenclaturalStatus (enum[])</li>
 * <li>scientificName</li>
 * </ul>
 */
public class ClasspathSourceList implements CloseableIterable<NubSource> {
    List<NubSource> sources = Lists.newArrayList();
    List<Future<NubSource>> futures = Lists.newArrayList();
    private final ExecutorService exec;

    /**
     * Creates a classpath based source that uses no resources at all.
     */
    public static ClasspathSourceList emptySource() {
        return new ClasspathSourceList();
    }

    /**
     * Creates a classpath based source that uses just the specieid classpath resources under /nub-sources
     */
    public static ClasspathSourceList source(Integer... datasetKeys) {
        return new ClasspathSourceList(Lists.newArrayList(datasetKeys));
    }

    private ClasspathSourceList() {
        exec = null;
    }

    private ClasspathSourceList(List<Integer> datasetKeys) {
        exec = Executors.newSingleThreadExecutor();
        for (Integer id : datasetKeys) {
            ClasspathSource src = new ClasspathSource(id);
            sources.add(src);
            futures.add(exec.submit(new LoadSource(src)));
        }
    }

    /**
     * Sets the higher rank setting of a given nub source which defaults to family if not set explicitly.
     */
    public void setSourceRank(int sourceId, Rank rank) {
        for (NubSource src : sources) {
            if (src.priority == sourceId) {
                src.ignoreRanksAbove = rank;
                break;
            }
        }
    }

    @Override
    public Iterator<NubSource> iterator() {
        return new FutureIterator(futures);
    }

    @Override
    public void close() throws Exception {
        exec.shutdownNow();
    }
}
