package org.gbif.nub.lookup;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.gbif.api.model.Constants;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;
import org.gbif.checklistbank.utils.CloseableUtils;
import org.gbif.nub.lookup.fuzzy.HigherTaxaComparator;
import org.gbif.nub.lookup.fuzzy.NubIndex;
import org.gbif.nub.lookup.fuzzy.NubMatchingServiceImpl;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.nub.lookup.straight.IdLookupPassThru;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * Guice module setting up all dependencies to expose the NubMatching service.
 * Requires a NameUsageMapper and a ClbConfiguration instance to be injectable
 */
public class NubMatchingModule extends PrivateModule implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(NubMatchingModule.class);
  private final File indexDir;
  private final UUID nubDatasetKey;
  private final ClbConfiguration cfg;
  private List<AutoCloseable> toBeClosed = Lists.newArrayList();

  /**
   * Creates a memory based nub index which is built from scratch every time the webservice starts up.
   */
  public NubMatchingModule() {
    this(null, Constants.NUB_DATASET_KEY, null);
  }

  /**
   * Creates a file based nub index which is built in case the index does not yet exist.
   *
   * @param indexDir      the directory to keep the lucene index in. If existing the index will be reused
   * @param nubDatasetKey the dataset key to use for populating the nub index
   */
  public NubMatchingModule(File indexDir, UUID nubDatasetKey, @Nullable Properties cfgProperties) {
    this.indexDir = indexDir;
    this.nubDatasetKey = nubDatasetKey;
    if (cfgProperties == null) {
      cfg = null;
    } else {
      cfg = ClbConfiguration.fromProperties(cfgProperties);
    }
  }

  @Override
  protected void configure() {
    bind(NubMatchingServiceImpl.class).asEagerSingleton();

    bind(NameUsageMatchingService.class).to(NubMatchingServiceImpl.class);
    bind(NameUsageMatchingService2.class).to(NubMatchingServiceImpl.class);

    expose(NameUsageMatchingService.class);
    expose(NameUsageMatchingService2.class);
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

  @Provides
  @Singleton
  public IdLookup provideLookup() throws IOException, SQLException {
    IdLookup lookup;
    if (cfg == null) {
      LOG.info("Using a pass through lookup");
      lookup = new IdLookupPassThru();

    } else if (indexDir == null) {
      LOG.info("Building a new temporary lookup db");
      lookup = IdLookupImpl.temp().load(cfg, false);

    } else {
      File ldb = new File(indexDir.getParentFile(), "nublookupDB");
      if (ldb.exists()) {
        LOG.info("Opening lookup db at {}", ldb.getAbsolutePath());
        lookup = IdLookupImpl.persistent(ldb);
      } else {
        FileUtils.forceMkdir(ldb.getParentFile());
        LOG.info("Creating Lookup db at {}", ldb.getAbsolutePath());
        lookup = IdLookupImpl.persistent(ldb).load(cfg, false);
      }
    }
    toBeClosed.add(lookup);
    return lookup;
  }

  @Override
  public void close() throws IOException {
    CloseableUtils.close(toBeClosed);
  }
}
