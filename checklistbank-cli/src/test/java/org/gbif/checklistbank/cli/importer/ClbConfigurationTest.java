package org.gbif.checklistbank.cli.importer;

import com.google.inject.Module;
import org.junit.Test;

public class ClbConfigurationTest {

  @Test
  public void testCreateServiceModule() throws Exception {
    ClbConfiguration cfg = new ClbConfiguration();
    cfg.url = "localhost";
    cfg.username = "test";
    cfg.password = "test";
    Module mod = cfg.createServiceModule();
  }
}