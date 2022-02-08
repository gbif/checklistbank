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
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.ClbDbLoadTestDataBeforeAll;
import org.gbif.checklistbank.service.mybatis.persistence.test.extensions.TestData;

import java.io.IOException;
import java.util.stream.Stream;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ContextConfiguration;

/** Test the Avro exporter using a hdfs mini cluster. */
@ExtendWith(ClbDbLoadTestDataBeforeAll.class)
@TestData(TestData.DATAFILE.SQUIRRELS)
@ContextConfiguration(initializers = {AvroExporterIT.ContextInitializer.class})
@Import(AvroExporterIT.TestConfig.class)
@Profile("test")
public class AvroExporterIT extends BaseIT {

  private ApplicationContext context;

  private final AvroExporter nameUsageAvroExporter;

  @Autowired
  public AvroExporterIT(ApplicationContext context, AvroExporter avroExporter) {
    this.nameUsageAvroExporter = avroExporter;
    this.context = context;
  }

  @Test
  public void testIndexBuild() throws IOException {
    nameUsageAvroExporter.run();
    FileStatus[] fileStatuses =
        context
            .getBean(MiniDFSCluster.class)
            .getFileSystem()
            .listStatus(new Path(HdfsTestUtil.TEST_HDFS_DIR));
    Assertions.assertNotNull(fileStatuses);
    Assertions.assertTrue(fileStatuses.length > 0);
  }

  public static class ContextInitializer extends BaseIT.ContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static MiniDFSCluster miniDFSCluster;

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      super.initialize(configurableApplicationContext);

      try {
        MiniDFSCluster miniDFSCluster = HdfsTestUtil.initHdfs();
        miniDFSCluster.getFileSystem().mkdirs(new Path(HdfsTestUtil.TEST_HDFS_DIR));

        TestPropertyValues.of(Stream.of(testPropertyPairs(miniDFSCluster)).toArray(String[]::new))
            .applyTo(configurableApplicationContext.getEnvironment());

        configurableApplicationContext.addBeanFactoryPostProcessor(
            configurableListableBeanFactory ->
                configurableListableBeanFactory.registerSingleton(
                    "MiniDFSCluster", miniDFSCluster));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    /** Creates the datasource and Solr settings from the embedded database. */
    String[] testPropertyPairs(MiniDFSCluster miniDFSCluster) {
      return new String[] {
        "checklistbank.indexer.nameNode=" + HdfsTestUtil.getNameNodeUri(miniDFSCluster),
        "checklistbank.indexer.targetHdfsDir=" + HdfsTestUtil.TEST_HDFS_DIR
      };
    }
  }

  @TestConfiguration
  @ComponentScan("org.gbif.checklistbank.index.backfill")
  public static class TestConfig {}
}
