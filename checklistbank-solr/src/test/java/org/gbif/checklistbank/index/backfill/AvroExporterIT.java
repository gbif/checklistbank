package org.gbif.checklistbank.index.backfill;

import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.solr.client.solrj.SolrServerException;
import org.gbif.checklistbank.index.AvroIndexingTestModule;
import org.gbif.checklistbank.index.HdfsTestUtil;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * Test the the Avro exporter using a hdfs mini cluster.
 */
public class AvroExporterIT {

  private static final Logger LOG = LoggerFactory.getLogger(AvroExporterIT.class);

  private static AvroExporter nameUsageAvroExporter;

  private static MiniDFSCluster miniDFSCluster;

  @BeforeClass
  public static void setup() throws IOException {
    //return"hdfs://localhost:"+ hdfsCluster.getNameNodePort() + "/";
    // run liquibase & dbSetup
    LOG.info("Run liquibase & dbSetup once");
    try {
      ClbDbTestRule rule = ClbDbTestRule.squirrels();
      rule.apply(new Statement() {
        @Override
        public void evaluate() throws Throwable {
          // do nothing
        }
      }, null).evaluate();
    } catch (Throwable throwable) {
      Throwables.propagate(throwable);
    }

    miniDFSCluster = HdfsTestUtil.initHdfs();
    // Creates the injector, merging properties taken from default test indexing and checklistbank
    Properties props = PropertiesUtil.loadProperties(IndexingConfigKeys.CLB_PROPERTY_FILE);
    Properties props2 = PropertiesUtil.loadProperties(IndexingConfigKeys.CLB_INDEXING_PROPERTY_TEST_FILE);
    props.putAll(props2);
    props.put(IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.NAME_NODE, HdfsTestUtil.getNameNodeUri(miniDFSCluster));
    props.put(IndexingConfigKeys.KEYS_INDEXING_CONF_PREFIX + IndexingConfigKeys.TARGET_HDFS_DIR, HdfsTestUtil.TEST_HDFS_DIR);
    miniDFSCluster.getFileSystem().mkdirs(new Path(HdfsTestUtil.TEST_HDFS_DIR));
    Injector injector = Guice.createInjector(new AvroIndexingTestModule(props));
    // Gets the exporter instance
    nameUsageAvroExporter = injector.getInstance(AvroExporter.class);
  }

  @AfterClass
  public static void shutdown(){
    miniDFSCluster.shutdown(false);
  }

  @Test
  public void testIndexBuild() throws IOException, SolrServerException, InterruptedException {
    nameUsageAvroExporter.run();
    FileStatus[] fileStatuses = miniDFSCluster.getFileSystem().listStatus(new Path(HdfsTestUtil.TEST_HDFS_DIR));
    Assert.assertNotNull(fileStatuses);
    Assert.assertTrue(fileStatuses.length > 0);

    // export avro file
    //Path localAvro = new Path("/Users/markus/avro-out");
    //miniDFSCluster.getFileSystem().copyToLocalFile(new Path(HdfsTestUtil.TEST_HDFS_DIR), localAvro);
    //System.out.println("Copied avro files to " + localAvro);
  }


}
