package org.gbif.checklistbank.index.backfill;

import org.gbif.checklistbank.index.HdfsTestUtil;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule2;

import java.io.IOException;
import javax.sql.DataSource;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/** Test the the Avro exporter using a hdfs mini cluster. */
public class AvroExporterIT extends BaseIT {

  private static final Logger LOG = LoggerFactory.getLogger(AvroExporterIT.class);

  private final AvroExporter nameUsageAvroExporter;

  private static MiniDFSCluster miniDFSCluster;

  @RegisterExtension public ClbDbTestRule2 sbSetup;

  @Autowired
  public AvroExporterIT(DataSource dataSource, AvroExporter nameUsageAvroExporter) {
    super(dataSource);
    this.nameUsageAvroExporter = nameUsageAvroExporter;
    sbSetup = ClbDbTestRule2.squirrels(dataSource);
  }

  @BeforeAll
  public static void setup() throws IOException {
    //   // return"hdfs://localhost:"+ hdfsCluster.getNameNodePort() + "/";
    //    // run liquibase & dbSetup
    //    LOG.info("Run liquibase & dbSetup once");
    //    try {
    //      // TODO: do in constructor?
    //      ClbDbTestRule rule = ClbDbTestRule.squirrels();
    //      rule.apply(
    //              new Statement() {
    //                @Override
    //                public void evaluate() throws Throwable {
    //                  // do nothing
    //                }
    //              },
    //              null)
    //          .evaluate();
    //    } catch (Throwable throwable) {
    //      Throwables.propagate(throwable);
    //    }

    miniDFSCluster = HdfsTestUtil.initHdfs();
    // Creates the injector, merging properties taken from default test indexing and checklistbank
    // TODO: move all props to application-test.yml?
//    Properties props = PropertiesUtil.loadProperties(IndexingConfigKeys.CLB_PROPERTY_FILE);
//    Properties props2 =
//        PropertiesUtil.loadProperties(IndexingConfigKeys.CLB_INDEXING_PROPERTY_TEST_FILE);
//    props.putAll(props2);
//    props.put(
//        IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.NAME_NODE,
//        HdfsTestUtil.getNameNodeUri(miniDFSCluster));
//    props.put(
//        IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.TARGET_HDFS_DIR,
//        HdfsTestUtil.TEST_HDFS_DIR);
    miniDFSCluster.getFileSystem().mkdirs(new Path(HdfsTestUtil.TEST_HDFS_DIR));
  }

  @AfterAll
  public static void shutdown() {
    miniDFSCluster.shutdown(false);
  }

  @Test
  public void testIndexBuild() throws IOException, SolrServerException, InterruptedException {
    nameUsageAvroExporter.run();
    FileStatus[] fileStatuses =
        miniDFSCluster.getFileSystem().listStatus(new Path(HdfsTestUtil.TEST_HDFS_DIR));
    Assert.assertNotNull(fileStatuses);
    Assert.assertTrue(fileStatuses.length > 0);

    // export avro file
    // Path localAvro = new Path("/Users/markus/avro-out");
    // miniDFSCluster.getFileSystem().copyToLocalFile(new Path(HdfsTestUtil.TEST_HDFS_DIR),
    // localAvro);
    // System.out.println("Copied avro files to " + localAvro);
  }
}
