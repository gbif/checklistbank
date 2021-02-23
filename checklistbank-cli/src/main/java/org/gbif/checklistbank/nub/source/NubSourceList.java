package org.gbif.checklistbank.nub.source;

import com.google.common.collect.Lists;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.iterable.CloseableIterable;
import org.gbif.nameparser.NameParserGbifV1;
import org.gbif.utils.concurrent.ExecutorUtils;
import org.gbif.utils.concurrent.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

/**
 * Base class for nub source lists that deals with the iterators and async loading of sources,
 * preserving the original source order.
 *
 * Note that you can only iterate the list once as the sources will be removed afterwards to free memory!
 */
public class NubSourceList implements CloseableIterable<NubSource> {
  private static final Logger LOG = LoggerFactory.getLogger(ClbSourceList.class);
  private final ExecutorService exec;
  private final NameParser parser;
  private List<Future<NubSource>> futures = Lists.newArrayList();
  protected final NubConfiguration cfg;

  public NubSourceList(NubConfiguration cfg) {
    this.cfg = cfg;
    parser = new NameParserGbifV1(cfg.parserTimeout);
    exec = new ThreadPoolExecutor(0, cfg.sourceLoaderThreads,
        100L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>(),
        new NamedThreadFactory("source-loader"));
  }

  /**
   * Call this method from subclasses once to submit all nub resources to this list.
   * The list will be submitted to a background loaders that calls init() on each NubSource asynchroneously.
   */
  public void submitSources(Iterable<? extends NubSource> sources) {
    // submit loader jobs
    int counter = 0;
    for (NubSource src : sources) {
      counter++;
      src.setParser(parser);
      futures.add(exec.submit(new LoadSource(src)));
    }

    LOG.info("Queued {} backbone sources for loading", counter);
  }

  @Override
  public Iterator<NubSource> iterator() {
    return new FutureIterator<NubSource>(futures);
  }

  @Override
  public void close() {
    ExecutorUtils.stop(exec, 1, TimeUnit.SECONDS);
  }

  /**
   * A simple wrapper for a list of futures providing an iterator for their results.
   * This allows to process futures in the order as they were originally submitted.
   */
  class FutureIterator<T> implements Iterator<T> {
    private final Iterator<Future<T>> iter;
    private Future<T> curr;

    public FutureIterator(Iterable<Future<T>> futures) {
      iter = futures.iterator();
    }

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public T next() {
      curr = iter.next();
      try {
        T obj = curr.get();
        // loaded, remove from sources to free up memory
        // see https://github.com/gbif/checklistbank/issues/165
        remove();
        //futures.remove(curr);
        return obj;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void remove() {
      iter.remove();
    }
  }

}
