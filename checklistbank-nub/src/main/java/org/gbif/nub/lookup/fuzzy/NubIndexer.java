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
package org.gbif.nub.lookup.fuzzy;

import org.gbif.checklistbank.service.mybatis.persistence.mapper.NameUsageMapper;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NubIndexer {

  private static final Logger LOG = LoggerFactory.getLogger(NubIndexer.class);

  private final NameUsageMapper mapper;
  private final File indexDir;
  private final UUID nubDatasetKey;

  @Autowired
  public NubIndexer(NameUsageMapper mapper,
                    @Value("${checklistbank.nub.indexDir:#{null}}") File indexDir,
                    @Value("${checklistbank.nub.datasetKey:d7dddbf4-2cf0-4f39-9b2a-bb099caae36c}") UUID nubDatasetKey) {
    this.mapper = mapper;
    this.indexDir = indexDir;
    this.nubDatasetKey =
      nubDatasetKey;
  }

  @Transactional
  public NubIndex index() throws IOException {
    NubIndex index;
    if (indexDir == null) {
      LOG.info("Starting building new nub memory index");
      index = NubIndex.newMemoryIndex(mapper);
      LOG.info("Lucene memory index initialized");
    } else {
      LOG.info("Starting building new nub file index in dir {}", indexDir);
      index = NubIndex.newFileIndex(indexDir, mapper, nubDatasetKey);
      LOG.info("Lucene file index initialized at {}", indexDir.getAbsolutePath());
    }
    return index;
  }
}
