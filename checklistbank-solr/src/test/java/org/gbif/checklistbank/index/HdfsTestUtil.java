package org.gbif.checklistbank.index;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.hdfs.MiniDFSCluster;

/**
 * Utility class to initialize a MiniDFSCluster a perform common operations required in test cases.
 */
public class HdfsTestUtil {

  public static final String TEST_HDFS_DIR = "clbsolrtest";

  /**
   * Initializes a MiniDFSCluster pointing to the directory  "./target/hdfs/";
   */
  public static MiniDFSCluster initHdfs() throws IOException {
    Configuration conf = new Configuration();
    File baseDir = new File("target/hdfs/").getAbsoluteFile();
    FileUtil.fullyDelete(baseDir);
    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
    MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
    return builder.build();
  }

  /**
   * Gets the name node URI running in the local host.
   */
  public static String getNameNodeUri(MiniDFSCluster MiniDFSCluster){
    return"hdfs://localhost:"+ MiniDFSCluster.getNameNodePort() + "/";
  }
}
