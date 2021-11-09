package org.gbif.checklistbank.cli.importer;

import org.gbif.utils.file.FileUtils;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import org.junit.Test;

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