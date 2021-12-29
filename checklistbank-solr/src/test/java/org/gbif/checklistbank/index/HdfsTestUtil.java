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
