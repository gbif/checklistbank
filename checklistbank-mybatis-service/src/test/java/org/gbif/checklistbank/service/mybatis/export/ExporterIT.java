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
package org.gbif.checklistbank.service.mybatis.export;

import org.gbif.api.model.Constants;
import org.gbif.api.model.registry.Dataset;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;

/**
 * Export squirrel test db as dwca
 */
public class ExporterIT {
  private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  @Rule
  public ClbDbTestRule sbSetup = ClbDbTestRule.squirrels();

  @Test
  public void testExport() throws Exception {
    ClbConfiguration cfg = mapper.readValue(Resources.getResource("clb-cfg.yaml"), ClbConfiguration.class);
    File repository = FileUtils.createTempDir();

    try {
      Exporter exp = Exporter.create(repository, cfg, "http://api.gbif.org/v1");
      exp.export(dataset(Constants.NUB_DATASET_KEY));

      exp.export(dataset(ClbDbTestRule.SQUIRRELS_DATASET_KEY));

    } finally {
      org.apache.commons.io.FileUtils.deleteDirectory(repository);
    }
  }

  private static Dataset dataset(UUID key) {
    Dataset d = new Dataset();
    d.setKey(key);
    d.setTitle("Dataset " + key);
    return d;
  }
}