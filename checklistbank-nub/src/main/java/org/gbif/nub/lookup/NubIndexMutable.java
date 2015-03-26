package org.gbif.nub.lookup;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.model.Usage;

import java.io.IOException;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TrackingIndexWriter;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A mutable memory based lucene index keeping the core attributes of a nub name usage.
 * The index exposes matching methods that allow to select usages based on their nub key or do fuzzy matches
 * on the canonical name alone.
 *
 * The index lies at the core of the nub matching service to preselect a list of potential good matches.
 *
 * For the entire nub with roughly 4.5 million usages this index requires 4GB of java memory.
 */
public class NubIndexMutable extends NubIndex {
  private static final Logger LOG = LoggerFactory.getLogger(NubIndexMutable.class);

  private final Directory index;
  private final IndexWriterConfig cfg;
  private final TrackingIndexWriter writer;
  private SearcherManager searchManager;
  private final ControlledRealTimeReopenThread<IndexSearcher> indexSearcherReopenThread;
  private long reopenToken;

  private NubIndexMutable() throws IOException {
    index = new RAMDirectory();
    LOG.info("Init a new, empty nub index");
    cfg = new IndexWriterConfig(Version.LATEST, analyzer);
    writer = new TrackingIndexWriter(new IndexWriter(index, cfg));
    // creates initial index segments
    writer.getIndexWriter().commit();
    searchManager = new SearcherManager(writer.getIndexWriter(), false, null);
    // Create the ControlledRealTimeReopenThread that reopens the index periodically having into
    // account the changes made to the index and tracked by the TrackingIndexWriter instance
    // The index is refreshed every 10 minutes when nobody is waiting
    // and every 10 millis whenever is someone waiting (see search method)
    indexSearcherReopenThread = new ControlledRealTimeReopenThread<IndexSearcher>(writer, searchManager, 600, 0.1);
    indexSearcherReopenThread.start(); // start the refresher thread
  }

  /**
   * Creates a new, empty nub index
   */
  public static NubIndexMutable newNubIndex() throws IOException {
    return new NubIndexMutable();
  }


  public void addNameUsage(NameUsage usage) throws IOException {
    reopenToken = writer.addDocument(toDoc(usage));
  }

  /**
   * Adds the minimal usage information to the index required for a lookup.
   * The returned match objects are lacking any classification information though, so make sure
   * to use an external classification resolver with the nub matching service.
   * Use the regular addNameUsage(usage) method for complete objects.
   * @param u minimal usage
   * @param canonical
   * @throws java.io.IOException
   */
  public void addNameUsage(Usage u, String canonical) throws IOException {
    reopenToken = writer.addDocument(toDoc(u.key, canonical, null, u.status, u.rank, null, null));
  }

  @Override
  protected IndexSearcher obtainSearcher() {
    try {
      indexSearcherReopenThread.waitForGeneration(reopenToken);
      return searchManager.acquire();

    } catch (IOException e) {
      LOG.error("Could not obtain lucene searcher", e);
      throw new RuntimeException(e);

    } catch (InterruptedException e) {
      LOG.error("Could not obtain lucene searcher in time", e);
      throw new RuntimeException(e);
    }
  }
}
