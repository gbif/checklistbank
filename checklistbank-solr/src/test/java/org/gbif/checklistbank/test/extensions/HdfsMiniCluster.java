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
package org.gbif.checklistbank.test.extensions;

import org.gbif.checklistbank.index.HdfsTestUtil;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Extension to handle MiniDFSCluster in a test class.
 */
public class HdfsMiniCluster implements AfterAllCallback, BeforeAllCallback {

  private MiniDFSCluster miniDFSCluster;

  @Override
  public void afterAll(ExtensionContext extensionContext) throws Exception {
    miniDFSCluster.shutdown(false);
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    miniDFSCluster = HdfsTestUtil.initHdfs();
    miniDFSCluster.getFileSystem().mkdirs(new Path(HdfsTestUtil.TEST_HDFS_DIR));
    GenericWebApplicationContext ctx = (GenericWebApplicationContext)SpringExtension.getApplicationContext(extensionContext);
    ctx.registerBean(MiniDFSCluster.class, () -> miniDFSCluster);
  }

  public MiniDFSCluster getMiniDFSCluster() {
    return miniDFSCluster;
  }
}
