package org.gbif.nub.lookup;

import org.apache.commons.io.FileUtils;
import org.gbif.api.model.Constants;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.NameUsageMapper;
import org.gbif.nub.lookup.fuzzy.HigherTaxaComparator;
import org.gbif.nub.lookup.fuzzy.NubIndex;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.nub.lookup.straight.IdLookupPassThru;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

/**
 * Guice module setting up all dependencies to expose the NubMatching service.
 * Requires a NameUsageMapper and a ClbConfiguration instance to be injectable
 */
@Configuration
public class NubMatchingConfigurationModule {
  private static final Logger LOG = LoggerFactory.getLogger(NubMatchingConfigurationModule.class);
  private final File indexDir;
  private final UUID nubDatasetKey;
  private final ClbConfiguration cfg;

  /**
   * Creates a memory based nub index which is built from scratch every time the webservice starts up.
   */
  public NubMatchingConfigurationModule() {
    this(null, Constants.NUB_DATASET_KEY, null);
  }

  /**
   * Creates a file based nub index which is built in case the index does not yet exist.
   *
   * @param indexDir      the directory to keep the lucene index in. If existing the index will be reused
   * @param nubDatasetKey the dataset key to use for populating the nub index
   */
  public NubMatchingConfigurationModule(File indexDir, UUID nubDatasetKey, @Nullable Properties cfgProperties) {
    this.indexDir = indexDir;
    this.nubDatasetKey = nubDatasetKey;
    if (cfgProperties == null) {
      cfg = null;
    } else {
      cfg = ClbConfiguration.fromProperties(cfgProperties);
    }
  }

  @Bean
  public NubIndex provideIndex(NameUsageMapper mapper) throws IOException {
    NubIndex index;
    if (indexDir == null) {
      index = NubIndex.newMemoryIndex(mapper);
      LOG.info("Lucene memory index initialized");
    } else {
      index = NubIndex.newFileIndex(indexDir, mapper, nubDatasetKey);
      LOG.info("Lucene file index initialized at {}", indexDir.getAbsolutePath());
    }
    return index;
  }

  @Bean
  public static HigherTaxaComparator provideSynonyms() {
    HigherTaxaComparator comp = new HigherTaxaComparator();
    LOG.info("Start loading synonym dictionaries from rs.gbif.org ...");
    comp.loadOnlineDicts();
    LOG.info("Online synonym dictionaries loaded");
    return comp;
  }

  @Bean
  public IdLookup provideLookup() {
    try {
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
      return lookup;

    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
