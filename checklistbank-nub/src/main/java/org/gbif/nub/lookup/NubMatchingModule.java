package org.gbif.nub.lookup;

import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.nameparser.NameParser;

import java.io.File;
import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice module setting up all dependencies to expose the NubMatching service.
 */
public class NubMatchingModule extends PrivateModule {
  private static final Logger LOG = LoggerFactory.getLogger(NubMatchingModule.class);
  private final int nubBuildThreads;
  private final File indexDir;

  /**
   * Creates a memory based nub index which is built from scratch every time the webservice starts up.
   */
  public NubMatchingModule() {
    this.nubBuildThreads = 4;
    this.indexDir = null;
  }

  /**
   * Creates a file based nub index which is built in case the index does not yet exist.
   * @param indexDir the directory to keep the lucene index in. If existing the index will be reused
   * @param nubBuildThreads number of threads to use for building a new index if needed
   */
  public NubMatchingModule(File indexDir, int nubBuildThreads) {
    this.nubBuildThreads = nubBuildThreads;
    this.indexDir = indexDir;
  }

  @Override
  protected void configure() {
    bind(NameUsageMatchingService.class).to(NubMatchingServiceImpl.class).asEagerSingleton();
    expose(NameUsageMatchingService.class);
  }

  @Provides
  @Inject
  @Singleton
  public NubIndex provideIndex(UsageService usageService) throws IOException {
    NubIndex index;
    if (indexDir == null) {
      index = NubIndexImmutable.newMemoryIndex(usageService, nubBuildThreads);
    } else {
      index = NubIndexImmutable.newFileIndex(indexDir, usageService, nubBuildThreads);
    }
    LOG.info("Lucene index initialized");
    return index;
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

  /*
    Removed due to http://dev.gbif.org/issues/browse/POR-2841
  @Provides
  @Singleton
  public static NameParser provideParser() {
    NameParser parser = new NameParser();
    return parser;
  }
  */
}
