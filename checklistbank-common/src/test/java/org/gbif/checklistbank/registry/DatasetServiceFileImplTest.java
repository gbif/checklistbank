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
package org.gbif.checklistbank.registry;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.utils.file.FileUtils;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatasetServiceFileImplTest {

  @Test
  public void get() throws Exception {

    DatasetService ds = new DatasetServiceFileImpl(FileUtils.getClasspathFile("registry-datasets.txt"));
    UUID key = UUID.fromString("c696e5ee-9088-4d11-bdae-ab88daffab78");
    assertEquals(key, ds.get(key).getKey());
    System.out.println(ds.get(key).toString());
  }

}