package org.gbif.checklistbank.cli.admin;

import java.io.File;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("manual test")
public class AdminCommandTest {

  @Test
  public void testCleanup() throws Exception {
    AdminCommand cmd = new AdminCommand();
    AdminConfiguration cfg = (AdminConfiguration) cmd.getConfigurationObject();
    cfg.archiveRepository = new File("/tmp/dwca");
    cfg.zookeeper.namespace = "uat_crawler";
    cfg.zookeeper.connectionString = "prodmaster1-vh.gbif.org,prodmaster2-vh.gbif.org,prodmaster3-vh.gbif.org";
    cfg.operation = AdminOperation.CLEANUP;
    cfg.key = UUID.fromString("cbb6498e-8927-405a-916b-576d00a6289b"); // wikipedia

    cmd.doRun();
  }

  @Test
  public void testCrawlPensoft() throws Exception {
    AdminCommand cmd = new AdminCommand();
    AdminConfiguration cfg = (AdminConfiguration) cmd.getConfigurationObject();
    cfg.messaging.host = "mq.gbif.org";
    cfg.messaging.virtualHost = "/prod";
    cfg.messaging.username = "clb";
    cfg.messaging.password = "clb";
    cfg.zookeeper.namespace = "prod_crawler";
    cfg.zookeeper.connectionString = "prodmaster1-vh.gbif.org,prodmaster2-vh.gbif.org,prodmaster3-vh.gbif.org";
    cfg.operation = AdminOperation.CRAWL;
    cfg.key = UUID.fromString("750a8724-fa66-4c27-b645-bd58ac5ee010"); // biodiversity journal publisher

    cmd.doRun();
  }
}