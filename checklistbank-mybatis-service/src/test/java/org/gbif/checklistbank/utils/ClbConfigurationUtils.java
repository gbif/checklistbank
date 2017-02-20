package org.gbif.checklistbank.utils;

import org.gbif.checklistbank.config.ClbConfiguration;

/**
 *
 */
public class ClbConfigurationUtils {

  public static ClbConfiguration local() {
    ClbConfiguration cfg = new ClbConfiguration();
    cfg.serverName = "localhost";
    cfg.databaseName = "checklistbank";
    cfg.user = "markus";
    cfg.password = "";
    return cfg;
  }

  public static ClbConfiguration uat() {
    ClbConfiguration cfg = local();
    cfg.serverName = "pg1.gbif-uat.org";
    cfg.databaseName = "uat_checklistbank";
    cfg.user = "clb";
    cfg.password = "---";
    return cfg;
  }

}
