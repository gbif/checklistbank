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
import org.gbif.checklistbank.index.BaseIT;
import org.gbif.checklistbank.index.HdfsTestUtil;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.ClbDbLoadTestDataBeforeAll;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.TestData;
import org.gbif.checklistbank.test.extensions.HdfsMiniCluster;

import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

/** Test the Avro exporter using a hdfs mini cluster. */
@ExtendWith(ClbDbLoadTestDataBeforeAll.class)
@TestData(TestData.DATAFILE.SQUIRRELS)
@ExtendWith(HdfsMiniCluster.class)
public class AvroExporterIT extends BaseIT {

  private final AvroExporter nameUsageAvroExporter;
  private final ApplicationContext context;

  @Autowired
  public AvroExporterIT(ApplicationContext context,
                        @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.THREADS + "}") String threads,
                        @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.BATCH_SIZE + "}") String batchSize,
                        @Value("${" + IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.LOG_INTERVAL + "}") String logInterval,
                        UsageService nameUsageService,
                        VernacularNameService vernacularNameService,
                        DescriptionService descriptionService,
                        DistributionService distributionService,
                        SpeciesProfileService speciesProfileService) {
    this.nameUsageAvroExporter = avroExporter(threads,
                                              batchSize,
                                              logInterval,
                                              context.getBean(MiniDFSCluster.class), // Injected by HdfsMiniCluster
                                              nameUsageService,
                                              vernacularNameService,
                                              descriptionService,
                                              distributionService,
                                              speciesProfileService);
    this.context = context;
  }

  private AvroExporter avroExporter(String threads, String batchSize, String logInterval,
    MiniDFSCluster miniDFSCluster,
    UsageService nameUsageService,
    VernacularNameService vernacularNameService,
    DescriptionService descriptionService,
    DistributionService distributionService,
    SpeciesProfileService speciesProfileService) {
    return new AvroExporter(threads, HdfsTestUtil.getNameNodeUri(miniDFSCluster), HdfsTestUtil.TEST_HDFS_DIR, batchSize, logInterval,nameUsageService,vernacularNameService, descriptionService, distributionService, speciesProfileService);
  }

  @Test
  public void testIndexBuild() throws IOException, SolrServerException, InterruptedException {
    nameUsageAvroExporter.run();
    FileStatus[] fileStatuses = context.getBean(MiniDFSCluster.class).getFileSystem().listStatus(new Path(HdfsTestUtil.TEST_HDFS_DIR));
    Assertions.assertNotNull(fileStatuses);
    Assertions.assertTrue(fileStatuses.length > 0);
  }
}
