
package org.gbif.checklistbank.service.mybatis.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import org.gbif.api.model.Constants;
import org.gbif.api.model.registry.Dataset;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.gbif.utils.file.FileUtils;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

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