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
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.utils.file.FileUtils;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNull;

public class FileRegistryModuleTest {

  @Test
  public void configure() throws Exception {

    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBean(FileRegistryModule.class, FileUtils.getClasspathFile("registry-datasets.txt"));
    ctx.refresh();

    DatasetService ds = ctx.getBean(DatasetService.class);
    assertNull(ds.get(UUID.randomUUID()));

    OrganizationService os = ctx.getBean(OrganizationService.class);
    assertNull(os.get(UUID.randomUUID()));
  }

}