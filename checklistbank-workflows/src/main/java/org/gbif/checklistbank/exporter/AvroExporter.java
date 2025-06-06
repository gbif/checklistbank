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
package org.gbif.checklistbank.exporter;

import lombok.SneakyThrows;

import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.service.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.VernacularNameServiceMyBatis;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checklist Bank multi-threaded name usage indexer. This class creates a pool of configurable
 * <i>threads</i> that concurrently execute a number of jobs each processing a configurable number
 * of name usages (<i>batchSize</i>) using a configurable number of concurrent lucene
 * <i>writers</i>. The indexer makes direct use of the mybatis layer and requires a checklist bank
 * datasource to be configured.
 */
@Component
public class AvroExporter extends NameUsageBatchProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(AvroExporter.class);

  private final String targetHdfsDir;

  @Autowired
  public AvroExporter(
      @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.THREADS + "}") Integer threads,
      @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.TARGET_HDFS_DIR + "}") String targetHdfsDir,
      @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.BATCH_SIZE + "}") Integer batchSize,
      @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.LOG_INTERVAL + "}") Integer logInterval,
      UsageService nameUsageService,
      VernacularNameService vernacularNameService,
      DescriptionService descriptionService,
      DistributionService distributionService,
      SpeciesProfileService speciesProfileService,
      FileSystem fileSystem) {
    super(
        threads,
        batchSize,
        logInterval,
        nameUsageService,
        vernacularNameService,
        descriptionService,
        distributionService,
        speciesProfileService,
        fileSystem);
    this.targetHdfsDir = targetHdfsDir;
  }

  @SneakyThrows
  @Override
  public int run() {
    // clear avro dir from old exports first
    LOG.info("Remove all content from target dir {}", targetHdfsDir);
    fileSystem.delete(new Path(targetHdfsDir), true);
    return super.run();
  }

  @Override
  protected Callable<Integer> newBatchJob(
    int startKey,
    int endKey,
    UsageService nameUsageService,
    VernacularNameServiceMyBatis vernacularNameService,
    DescriptionServiceMyBatis descriptionService,
    DistributionServiceMyBatis distributionService,
    SpeciesProfileServiceMyBatis speciesProfileService,
    FileSystem fileSystem
    ) {
    return new AvroExportJob(
        nameUsageService,
        startKey,
        endKey,
        vernacularNameService,
        descriptionService,
        distributionService,
        speciesProfileService,
        fileSystem,
        targetHdfsDir);
  }
}
