package org.gbif.nub.lookup;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.checklistbank.index.ThreadPoolRunner;
import org.gbif.checklistbank.service.UsageService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A multithreaded nub index builder reading the backbone from a checkist bank postgres database in batches.
 * It first reads out all usage id ints and partitions those in equal ranges of 20.000
 */
public class NubIndexBuilder extends ThreadPoolRunner<List<NameUsage>> {

  private static final Logger LOG = LoggerFactory.getLogger(NubIndexBuilder.class);

  private final IndexWriter writer;
  private AtomicInteger counter = new AtomicInteger();
  private UsageService usageService;
  private int offset;
  private final int max;
  // the size of the key range used to get usages.
  // Not all keys exist and they are not evenly distributed, so this is the max of usages returned, but its usually less
  private final int window;

  NubIndexBuilder(IndexWriter writer, UsageService usageService, int threads) {
    super(threads);
    this.writer = writer;
    this.usageService = usageService;
    this.offset = 0;
    this.max = usageService.maxUsageKey(Constants.NUB_DATASET_KEY);
    this.window = 20000;
    LOG.info("Retrieved biggest nub usage key={}. Building the index from {} batches with {} usages each", max, max/window, window);
    // use sth like the following for local debugging: (puma concolor id)
    //this.window = 100;
    //this.offset = 2435099-window;
    //this.max = offset + 2*window;
  }

  @Override
  protected Callable<List<NameUsage>> newJob() {
    if (offset >= max) {
      // no more jobs!
      return null;
    }
    int start = offset;
    int end = Math.min(offset + window - 1, max);
    this.offset = end + 1;
    return new ReadUsageBatch(start, end, usageService);
  }

  @Override
  /**
   * Consumes the batch results and inserts them into the index.
   */
  protected void taskResponseHook(List<NameUsage> usages) {
    LOG.debug("Add {} usages to index", usages.size());
    if (!usages.isEmpty()) {
      LOG.debug("First usage is: {}", usages.get(0).getScientificName());
    }
    for (NameUsage u : usages) {
      try {
        writer.addDocument(NubIndex.toDoc(u));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    counter.getAndAdd(usages.size());
    LOG.info("{} usages added to nub index, {} in total", usages.size(), counter);
  }


  public class ReadUsageBatch implements Callable<List<NameUsage>> {
      private final int start;
      private final int end;
      private final UsageService usageService;

      public ReadUsageBatch(int start, int end, UsageService usageService) {
        this.start = start;
        this.end = end;
        this.usageService = usageService;
      }

      @Override
      public List<NameUsage> call() throws Exception {
        LOG.debug("Retrieve usages from CLB in range {}-{}", start, end);
        List<NameUsage> usages = usageService.listRange(start, end);
        LOG.debug("Retrieved {} usages from CLB in range {}-{}", usages.size(), start, end);
        return usages;
      }
    }
}
