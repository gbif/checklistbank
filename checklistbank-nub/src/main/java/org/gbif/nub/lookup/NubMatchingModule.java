package org.gbif.nub.lookup;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.gbif.api.model.Constants;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;
import org.gbif.checklistbank.utils.CloseableUtils;
import org.gbif.nub.lookup.fuzzy.HigherTaxaComparator;
import org.gbif.nub.lookup.fuzzy.NubIndex;
import org.gbif.nub.lookup.fuzzy.NubMatchingServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Guice module setting up all dependencies to expose the NubMatching service.
 * Requires a NameUsageMapper and a ClbConfiguration instance to be injectable
 */
public class NubMatchingModule extends PrivateModule implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(NubMatchingModule.class);
  private final File indexDir;
  private final UUID nubDatasetKey;
  private List<AutoCloseable> toBeClosed = Lists.newArrayList();

  /**
   * Creates a memory based nub index which is built from scratch every time the webservice starts up.
   */
  public NubMatchingModule() {
    this(null, Constants.NUB_DATASET_KEY);
  }

  /**
   * Creates a file based nub index which is built in case the index does not yet exist.
   *
   * @param indexDir      the directory to keep the lucene index in. If existing the index will be reused
   * @param nubDatasetKey the dataset key to use for populating the nub index
   */
  public NubMatchingModule(File indexDir, UUID nubDatasetKey) {
    this.indexDir = indexDir;
    this.nubDatasetKey = nubDatasetKey;
  }

  @Override
  protected void configure() {
    bind(NubMatchingServiceImpl.class).asEagerSingleton();

    bind(NameUsageMatchingService.class).to(NubMatchingServiceImpl.class);
    bind(NameUsageMatchingService2.class).to(NubMatchingServiceImpl.class);

    expose(NameUsageMatchingService.class);
    expose(NameUsageMatchingService2.class);
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
      index = NubIndex.newFileIndex(indexDir, mapper, nubDatasetKey);
      LOG.info("Lucene file index initialized at {}", indexDir.getAbsolutePath());
    }
    toBeClosed.add(index);
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

  @Override
  public void close() throws IOException {
    CloseableUtils.close(toBeClosed);
  }
}
