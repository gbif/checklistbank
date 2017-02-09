package org.gbif.checklistbank.cli.exporter;

import org.gbif.api.model.Constants;
import org.gbif.api.model.registry.Dataset;
import org.gbif.checklistbank.cli.admin.AdminCommand;
import org.gbif.checklistbank.cli.admin.AdminConfiguration;
import org.gbif.checklistbank.cli.admin.AdminOperation;
import org.gbif.checklistbank.config.ClbConfiguration;

import java.io.File;
import java.util.UUID;

import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;
import org.gbif.checklistbank.utils.ClbConfigurationUtils;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
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