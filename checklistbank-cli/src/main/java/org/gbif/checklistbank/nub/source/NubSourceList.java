package org.gbif.checklistbank.nub.source;

import com.google.common.collect.Lists;
import org.gbif.checklistbank.iterable.CloseableIterable;
import org.gbif.checklistbank.iterable.FutureIterator;
import org.gbif.utils.concurrent.ExecutorUtils;
import org.gbif.utils.concurrent.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Base class for nub source lists that deals with the iterators and async loading of sources.
 */
public class NubSourceList implements CloseableIterable<NubSource> {
  private static final Logger LOG = LoggerFactory.getLogger(ClbSourceList.class);
  protected final ExecutorService exec;
  protected List<Future<NubSource>> futures = Lists.newArrayList();
  private final boolean parseNames;

  public NubSourceList(boolean parseNames) {
    this.parseNames = parseNames;
    //exec = new DirectExecutor();
    exec = Executors.newSingleThreadExecutor(new NamedThreadFactory("source-loader"));
  }

  public NubSourceList(Iterable<? extends NubSource> sources, boolean parseNames) {
    this(parseNames);
    submitSources(sources);
  }

  /**
   * Call this method from subclasses once to submit all nub resources to this list.
   * The list will be submitted to a background loaders that calls init() on each NubSource asynchroneously.
   */
  protected void submitSources(Iterable<? extends NubSource> sources) {
    // submit loader jobs
    int counter = 0;
    for (NubSource src : sources) {
      counter++;
      futures.add(exec.submit(new LoadSource(src, parseNames)));
    }
    LOG.info("Queued {} backbone sources for loading", counter);
  }

  @Override
  public Iterator<NubSource> iterator() {
    return new FutureIterator<NubSource>(futures);
  }

  @Override
  public void close() {
    ExecutorUtils.stop(exec, 10, TimeUnit.SECONDS);
  }
}
