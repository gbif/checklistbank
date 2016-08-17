package org.gbif.checklistbank.cli.exporter;

import org.gbif.api.model.Constants;
import org.gbif.api.model.registry.Dataset;
import org.gbif.checklistbank.config.ClbConfiguration;

import java.io.File;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
@Ignore("manual test")
public class ExporterTest {

  @Test
  public void testExport() throws Exception {
    ClbConfiguration cfg = new ClbConfiguration();
    File repository = new File("/Users/markus/Desktop/dwcas");
    cfg.serverName = "localhost";
    cfg.databaseName = "clb";
    cfg.user = "postgres";
    cfg.password = "pogo";

    cfg.serverName = "pg1.gbif.org";
    cfg.databaseName = "checklistbank";
    cfg.user = "clb";
    cfg.password = "%BBJu2MgstXJ";

    Exporter exp = Exporter.create(repository, cfg, "http://api.gbif.org/v1");
    exp.export(dataset(Constants.NUB_DATASET_KEY));
  }

  private static Dataset dataset(UUID key) {
    Dataset d = new Dataset();
    d.setKey(key);
    d.setTitle("Dataset " + key);
    return d;
  }
}