/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nub.lookup;

import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.NameUsageMapper;
import org.gbif.nub.lookup.fuzzy.HigherTaxaComparator;
import org.gbif.nub.lookup.fuzzy.NubIndex;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.nub.lookup.straight.IdLookupPassThru;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Guice module setting up all dependencies to expose the NubMatching service.
 * Requires a NameUsageMapper and a ClbConfiguration instance to be injectable
 */
@Configuration
public class NubMatchingConfigurationModule {

  private static final Logger LOG = LoggerFactory.getLogger(NubMatchingConfigurationModule.class);

  @Bean
  public NubIndex provideIndex(NameUsageMapper mapper,
                              @Value("${checklistbank.nub.indexDir:#{null}}") File indexDir,
                              @Value("${checklistbank.nub.datasetKey:d7dddbf4-2cf0-4f39-9b2a-bb099caae36c}") UUID nubDatasetKey) throws IOException {
    NubIndex index;
    if (indexDir == null) {
      LOG.info("Starting building new nub memory index in dir {}", indexDir);
      index = NubIndex.newMemoryIndex(mapper);
      LOG.info("Lucene memory index initialized");
    } else {
      LOG.info("Starting building new nub file index");
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
  public IdLookup provideLookup(@Value("${checklistbank.nub.indexDir:#{null}}") File indexDir, ClbConfiguration cfg) {
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
