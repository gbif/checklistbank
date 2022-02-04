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
package org.gbif.checklistbank.index.backfill;

import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.service.*;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checklist Bank multi-threaded name usage solr indexer. This class creates a pool of configurable
 * <i>threads</i> that concurrently execute a number of jobs each processing a configurable number
 * of name usages (<i>batchSize</i>) using a configurable number of concurrent lucene
 * <i>writers</i>. The indexer makes direct use of the mybatis layer and requires a checklist bank
 * datasource to be configured.
 */
@Component
public class AvroExporter extends NameUsageBatchProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(AvroExporter.class);

  private final String nameNode;
  private final String targetHdfsDir;

  @Autowired
  public AvroExporter(
      @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.THREADS + "}") Integer threads,
      @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.NAME_NODE + "}") String nameNode,
      @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.TARGET_HDFS_DIR + "}") String targetHdfsDir,
      @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.BATCH_SIZE + "}") Integer batchSize,
      @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.LOG_INTERVAL + "}") Integer logInterval,
      UsageService nameUsageService,
      VernacularNameService vernacularNameService,
      DescriptionService descriptionService,
      DistributionService distributionService,
      SpeciesProfileService speciesProfileService) {

    super(
        threads,
        batchSize,
        logInterval,
        nameUsageService,
        vernacularNameService,
        descriptionService,
        distributionService,
        speciesProfileService);
    this.nameNode = nameNode;
    this.targetHdfsDir = targetHdfsDir;
  }

  @Override
  protected Callable<Integer> newBatchJob(
      int startKey,
      int endKey,
      UsageService nameUsageService,
      VernacularNameServiceMyBatis vernacularNameService,
      DescriptionServiceMyBatis descriptionService,
      DistributionServiceMyBatis distributionService,
      SpeciesProfileServiceMyBatis speciesProfileService) {
    return new AvroExportJob(
        nameUsageService,
        startKey,
        endKey,
        vernacularNameService,
        descriptionService,
        distributionService,
        speciesProfileService,
        nameNode,
        targetHdfsDir);
  }
}
