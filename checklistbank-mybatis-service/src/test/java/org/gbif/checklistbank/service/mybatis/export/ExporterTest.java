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
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbLoadTestDb;

import java.io.File;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("manual test")
public class ExporterTest {

  @Test
  public void testExport() throws Exception {
    File repository = new File("/Users/markus/Desktop/dwcas");

    Exporter exp = new Exporter(repository, null, null);
    exp.export(dataset(Constants.NUB_DATASET_KEY));

    exp.export(dataset(ClbLoadTestDb.SQUIRRELS_DATASET_KEY));
  }

  private static Dataset dataset(UUID key) {
    Dataset d = new Dataset();
    d.setKey(key);
    d.setTitle("Dataset " + key);
    return d;
  }
}
