package org.gbif.checklistbank.utils;

import org.gbif.checklistbank.config.ClbConfiguration;

/**
 *
 */
public class ClbConfigurationUtils {

  public static ClbConfiguration local() {
    ClbConfiguration cfg = new ClbConfiguration();
    cfg.serverName = "localhost";
    cfg.databaseName = "clb";
    cfg.user = "postgres";
    cfg.password = "pogo";
    return cfg;
  }

  public static ClbConfiguration uat() {
    ClbConfiguration cfg = local();
    cfg.serverName = "pg1.gbif-uat.org";
    cfg.databaseName = "nubtest";
    cfg.user = "clb";
    cfg.password = "%BBJu2MgstXJ";
    return cfg;
  }

}
