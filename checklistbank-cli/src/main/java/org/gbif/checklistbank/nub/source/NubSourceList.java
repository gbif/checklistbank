package org.gbif.checklistbank.nub.source;

import org.gbif.checklistbank.concurrent.NamedThreadFactory;
import org.gbif.checklistbank.iterable.CloseableIterable;
import org.gbif.checklistbank.iterable.FutureIterator;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for nub source lists that deals with the iteratore and async loading of sources.
 */
public abstract class NubSourceList implements CloseableIterable<NubSource> {
    private static final Logger LOG = LoggerFactory.getLogger(ClbSourceList.class);
    protected final ExecutorService exec;
    protected List<Future<NubSource>> futures = Lists.newArrayList();

    public NubSourceList() {
        exec = Executors.newSingleThreadExecutor(new NamedThreadFactory("source-loader"));
    }

    /**
     * Call this method once to submit all nub resources to this list.
     * The list will get ordered by priority and then submitted to a background loaders that calls init() on each NubSource asynchroneously.
     * @param sources
     */
    protected void submitSources(Collection<? extends NubSource> sources) {
        LOG.info("Found {} backbone sources", sources.size());

        // sort source according to priority and date created (e.g. for org datasets!)
        Ordering<NubSource> order = Ordering
                .natural()
                .onResultOf(new Function<NubSource, Integer>() {
                    @Nullable
                    @Override
                    public Integer apply(NubSource input) {
                        return input.priority;
                    }
                })
                .compound(Ordering
                                .natural()
                                .reverse()  // newest first, e.g. pensoft articles
                                .nullsLast()
                                .onResultOf(new Function<NubSource, Date>() {
                                    @Nullable
                                    @Override
                                    public Date apply(NubSource input) {
                                        return input.created;
                                    }
                                })
                );
        // submit loader jobs
        ExecutorCompletionService ecs = new ExecutorCompletionService(exec);
        for (NubSource src : order.sortedCopy(sources)) {
            futures.add(ecs.submit(new LoadSource(src)));
        }
    }

    @Override
    public Iterator<NubSource> iterator() {
        return new FutureIterator<NubSource>(futures);
    }

    @Override
    public void close() throws Exception {
        exec.shutdownNow();
    }
}
