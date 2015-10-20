package org.gbif.nub.lookup;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.checklistbank.service.UsageService;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A read only lucene index keeping the core attributes of a nub name usage.
 * The index exposes matching methods that allow to select usages based on their nub key or do fuzzy matches
 * on the canonical name alone.
 *
 * The index lies at the core of the nub matching service to preselect a list of potential good matches.
 *
 * The immutable index can either be purely memory based or on the filesystem using a memory mapped OS cache.
 * For the entire nub with roughly 4.5 million usages this index requires 4GB of heap memory if the RAMDirectory is used.
 * The memory mapped file index uses very little heap memory and instead all available memory should be given to the OS
 * to enabling caching on the file system level.
 */
public class NubIndexImmutable extends NubIndex {
  private static final Logger LOG = LoggerFactory.getLogger(NubIndexImmutable.class);

  private final Directory index;
  private final IndexSearcher searcher;

  private NubIndexImmutable(Directory d) throws IOException {
    index = d;
    DirectoryReader reader= DirectoryReader.open(index);
    searcher = new IndexSearcher(reader);
  }

  private static void load(Directory d, UsageService usageService, int threads) throws IOException {
    LOG.info("Start building a new nub index");
    IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
    IndexWriter writer = new IndexWriter(d, cfg);
    // creates initial index segments
    writer.commit();

    NubIndexBuilder builder = new NubIndexBuilder(writer, usageService, threads);
    builder.run();

    writer.close();
    LOG.info("Finished building nub index");
  }

  public static NubIndexImmutable newMemoryIndex(UsageService usageService, int threads) throws IOException {
    RAMDirectory dir = new RAMDirectory();
    load(dir, usageService, threads);
    return new NubIndexImmutable(dir);
  }

  public static NubIndexImmutable newMemoryIndex(Iterable<NameUsageMatch> usages) throws IOException {
    LOG.info("Start building a new nub RAM index");
    RAMDirectory dir = new RAMDirectory();
    IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
    IndexWriter writer = new IndexWriter(dir, cfg);
    // creates initial index segments
    writer.commit();
    int counter = 0;
    for (NameUsageMatch u : usages) {
      if (u != null && u.getUsageKey() != null) {
        writer.addDocument(toDoc(u));
        counter++;
      }
    }
    writer.close();
    LOG.info("Finished building nub index with {} usages", counter);
    return new NubIndexImmutable(dir);
  }


  /**
   * Creates a nub index for the backbone by loading it from the lucene index dir if it exists.
   * If it is not existing a new index directory will be built using the UsageService with given threads.
   * If the indexDir is null the index is never written to the filesytem but just kept in memory.
   * @param indexDir directory to use as the lucence index directory. If null the index is only kept in memory.
   */
  public static NubIndexImmutable newFileIndex(File indexDir, UsageService usageService, int threads) throws IOException {
    MMapDirectory dir;
    if (indexDir.exists()) {
      Preconditions.checkArgument(indexDir.isDirectory(), "Given index directory exists but is not a directory");
      // load existing index from disk
      LOG.info("Loading existing nub index from disk: {}", indexDir.getAbsoluteFile());
      dir = new MMapDirectory(indexDir.toPath());

    } else {
      // create new memory mapped file based index and then populate it
      LOG.info("Creating new nub index directory at {}", indexDir.getAbsoluteFile());
      FileUtils.forceMkdir(indexDir);
      dir = new MMapDirectory(indexDir.toPath());
      load(dir, usageService, threads);
    }
    return new NubIndexImmutable(dir);
  }

  @Override
  protected IndexSearcher obtainSearcher() {
    return searcher;
  }

}
