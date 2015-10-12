package org.gbif.checklistbank.index;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.hdfs.MiniDFSCluster;

public class HdfsTestUtil {

  public static final String TEST_HDFS_DIR = "clbsolrtest";

  public static String initHdfs() throws IOException {
    Configuration conf = new Configuration();
    File baseDir = new File("./target/hdfs/").getAbsoluteFile();
    FileUtil.fullyDelete(baseDir);
    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
    MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
    MiniDFSCluster hdfsCluster = builder.build();
    return"hdfs://localhost:"+ hdfsCluster.getNameNodePort() + "/";
  }
}
