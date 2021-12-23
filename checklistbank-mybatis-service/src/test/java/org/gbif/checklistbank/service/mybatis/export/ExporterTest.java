package org.gbif.checklistbank.service.mybatis.export;

import org.gbif.api.model.Constants;
import org.gbif.api.model.registry.Dataset;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.service.mybatis.persistence.postgres.ClbDbTestRule;
import org.gbif.checklistbank.utils.ClbConfigurationUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.UUID;


@Ignore("manual test")
public class ExporterTest {

  @Test
  public void testExport() throws Exception {
    ClbConfiguration cfg = ClbConfigurationUtils.local();
    File repository = new File("/Users/markus/Desktop/dwcas");

    Exporter exp = Exporter.create(repository, cfg, "http://api.gbif.org/v1");
    exp.export(dataset(Constants.NUB_DATASET_KEY));

    exp.export(dataset(ClbDbTestRule.SQUIRRELS_DATASET_KEY));
  }

  private static Dataset dataset(UUID key) {
    Dataset d = new Dataset();
    d.setKey(key);
    d.setTitle("Dataset " + key);
    return d;
  }
}