package org.gbif.nub.lookup;

import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;
import org.gbif.checklistbank.utils.CloseableUtils;
import org.gbif.nub.lookup.fuzzy.HigherTaxaComparator;
import org.gbif.nub.lookup.fuzzy.NubIndex;
import org.gbif.nub.lookup.fuzzy.NubMatchingServiceImpl;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice module setting up all dependencies to expose the NubMatching service.
 * Requires a NameUsageMapper and a ClbConfiguration instance to be injectable
 */
public class NubMatchingModule extends PrivateModule implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(NubMatchingModule.class);
  private final File indexDir;
  private final boolean incDeleted;
  private List<AutoCloseable> toBeClosed = Lists.newArrayList();

  /**
   * Creates a memory based nub index which is built from scratch every time the webservice starts up.
   */
  public NubMatchingModule() {
    this.indexDir = null;
    this.incDeleted = false;
  }

  /**
   * Creates a file based nub index which is built in case the index does not yet exist.
   * @param indexDir the directory to keep the lucene index in. If existing the index will be reused
   */
  public NubMatchingModule(File indexDir, boolean incDeleted) {
    this.indexDir = indexDir;
    this.incDeleted = incDeleted;
  }

  /**
   * Dummy class to force the idlookup to be eagerly created
   * Otherwise a new index is not built on webservice startup but only when a request is made
   */
  static class EagerIdLookupLoader {
    @Inject
    IdLookup lookup;
  }

  @Override
  protected void configure() {
    bind(NameUsageMatchingService.class).to(NubMatchingServiceImpl.class).asEagerSingleton();
    bind(EagerIdLookupLoader.class).asEagerSingleton();
    expose(NameUsageMatchingService.class);
    expose(IdLookup.class);
  }

  @Provides
  @Inject
  @Singleton
  public NubIndex provideIndex(NameUsageMapper mapper) throws IOException {
    NubIndex index;
    if (indexDir == null) {
      index = NubIndex.newMemoryIndex(mapper);
      LOG.info("Lucene memory index initialized");
    } else {
      index = NubIndex.newFileIndex(indexDir, mapper);
      LOG.info("Lucene file index initialized at {}", indexDir.getAbsolutePath());
    }
    toBeClosed.add(index);
    return index;
  }

  @Provides
  @Inject
  @Singleton
  public IdLookup provideStraightLookup(ClbConfiguration clb) throws Exception {
    IdLookupImpl lookup;
    boolean load = true;
    try {
      if (indexDir == null) {
        lookup = IdLookupImpl.temp();
        LOG.info("Temporary IdLookup created");
      } else {
        File db = new File(indexDir, "idlookup");
        load = !db.exists();
        lookup = IdLookupImpl.persistent(db);
        LOG.info("New empty, persistent IdLookup created with file map at {}", db.getAbsolutePath());
      }
      if (load) {
        LOG.info("Loading usages into IdLookup ...");
        lookup.load(clb, incDeleted);
      }
    } catch (Exception e) {
      LOG.error("Failed to create IdLookup at {}", indexDir, e);
      throw e;
    }
    toBeClosed.add(lookup);
    return lookup;
  }

  @Provides
  @Singleton
  public static HigherTaxaComparator provideSynonyms() {
    HigherTaxaComparator comp = new HigherTaxaComparator();
    LOG.info("Start loading synonym dictionaries from rs.gbif.org ...");
    comp.loadOnlineDicts();
    LOG.info("Online synonym dictionaries loaded");
    return comp;
  }

  @Override
  public void close() throws IOException {
    CloseableUtils.close(toBeClosed);
  }
}
