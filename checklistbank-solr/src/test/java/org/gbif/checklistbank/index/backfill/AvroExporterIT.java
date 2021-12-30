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

import org.gbif.checklistbank.index.BaseIT;
import org.gbif.checklistbank.index.HdfsTestUtil;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;

import java.io.IOException;

import javax.sql.DataSource;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

/** Test the Avro exporter using a hdfs mini cluster. */
public class AvroExporterIT extends BaseIT {

  private final AvroExporter nameUsageAvroExporter;

  @RegisterExtension
  public ClbDbTestRule sbSetup;

  @Autowired
  public AvroExporterIT(DataSource dataSource, AvroExporter nameUsageAvroExporter) {
    sbSetup = ClbDbTestRule.squirrels(dataSource);
    this.nameUsageAvroExporter = nameUsageAvroExporter;
  }

  @Test
  public void testIndexBuild() throws IOException, SolrServerException, InterruptedException {
    nameUsageAvroExporter.run();
    FileStatus[] fileStatuses =
        miniDFSCluster.getFileSystem().listStatus(new Path(HdfsTestUtil.TEST_HDFS_DIR));
    Assertions.assertNotNull(fileStatuses);
    Assertions.assertTrue(fileStatuses.length > 0);
  }
}
