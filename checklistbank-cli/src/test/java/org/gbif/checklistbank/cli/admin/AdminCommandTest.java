package org.gbif.checklistbank.cli.admin;

import java.io.File;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("manual test")
public class AdminCommandTest {

  @Test
  public void testDoRun() throws Exception {
    AdminCommand cmd = new AdminCommand();
    AdminConfiguration cfg = (AdminConfiguration) cmd.getConfigurationObject();
    cfg.archiveRepository = new File("/tmp/dwca");
    cfg.zookeeper.namespace = "uat_crawler";
    cfg.zookeeper.connectionString = "prodmaster1-vh.gbif.org,prodmaster2-vh.gbif.org,prodmaster3-vh.gbif.org";
    cfg.operation = AdminOperation.CLEANUP;
    cfg.datasetKey = UUID.fromString("cbb6498e-8927-405a-916b-576d00a6289b"); // wikipedia

    cmd.doRun();
  }
}