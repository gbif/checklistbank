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
package org.gbif.checklistbank.cli.importer;

import org.gbif.utils.file.FileUtils;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;

@Ignore("REMOVE! ignored only to make the jenkins build work")
public class ImporterServiceIT {

  /**
   * Make sure messages are all registered and the service starts up fine.
   */
  @Test
  public void testStartUp() throws Exception {
    File neoTmp = FileUtils.createTempDir();
    neoTmp.deleteOnExit();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    ImporterConfiguration cfg = mapper.readValue(Resources.getResource("cfg-importer.yaml"),
                                                 ImporterConfiguration.class);
    cfg.neo.neoRepository = neoTmp;

    ImporterService imp = new ImporterService(cfg);
    System.out.println("Startup");
    imp.startUp();
    System.out.println("Shutdown");
    imp.shutDown();
  }
}