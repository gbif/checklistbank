/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.cli.admin;

import org.gbif.api.model.Constants;

import java.io.File;
import java.util.UUID;

import org.junit.Test;
import org.junit.jupiter.api.Disabled;

@Disabled("manual test")
public class AdminCommandTest {

  private AdminConfiguration configureLocal(AdminConfiguration cfg) throws Exception {
    cfg.archiveRepository = new File("/tmp/dwca");

    cfg.zookeeper.namespace = "uat_crawler";
    cfg.zookeeper.connectionString = "prodmaster1-vh.gbif.org,prodmaster2-vh.gbif.org,prodmaster3-vh.gbif.org";

//    cfg.clb.serverName = "localhost";
//    cfg.clb.databaseName = "checklistbank";
    cfg.clb.user = "markus";
    cfg.clb.password = "";

    cfg.messaging.host = "localhost";
    cfg.messaging.virtualHost = "/";
    cfg.messaging.username = "guest";
    cfg.messaging.password = "guest";
    return cfg;
  }

  private AdminConfiguration configureUat(AdminConfiguration cfg) throws Exception {
    configureLocal(cfg);
//    cfg.clb.serverName = "pg1.gbif-uat.org";
//    cfg.clb.databaseName = "uat_checklistbank";
    cfg.clb.user = "clb";
    cfg.clb.password = "xxx";

    cfg.messaging.host = "mq.gbif.org";
    cfg.messaging.virtualHost = "/uat";
    cfg.messaging.username = "clb";
    cfg.messaging.password = "clb";
    return cfg;
  }
  
  private AdminConfiguration configureNub(AdminConfiguration cfg) throws Exception {
    configureLocal(cfg);
  
    cfg.zookeeper.namespace = "dev_crawler";
    cfg.zookeeper.connectionString = "c3zk1.gbif-dev.org,c3zk2.gbif-dev.org,c3zk3.gbif-dev.org";
    cfg.messaging.host = "mq.gbif.org";
    cfg.messaging.virtualHost = "/users/mdoering";
    cfg.messaging.username = "clb";
    cfg.messaging.password = "clb";
    return cfg;
  }


  @Test
  public void runCleanup() throws Exception {
    AdminCommand cmd = new AdminCommand();
    AdminConfiguration cfg = configureLocal( (AdminConfiguration) cmd.getConfigurationObject());
    cfg.operation = AdminOperation.CLEANUP;
    cfg.key = UUID.fromString("cbb6498e-8927-405a-916b-576d00a6289b"); // wikipedia

    cmd.doRun();
  }
  
  @Test
  public void runAssertion() throws Exception {
    AdminCommand cmd = new AdminCommand();
    AdminConfiguration cfg = configureUat( (AdminConfiguration) cmd.getConfigurationObject());
    cfg.operation = AdminOperation.VALIDATE_BACKBONE;
    
    cmd.doRun();
  }
  
  @Test
  public void nubMatchAll() throws Exception {
    AdminCommand cmd = new AdminCommand();
    AdminConfiguration cfg = configureNub( (AdminConfiguration) cmd.getConfigurationObject());
    cfg.operation = AdminOperation.REMATCH;
    cmd.doRun();
  }
  
  @Test
  public void nubMatchOne() throws Exception {
    AdminCommand cmd = new AdminCommand();
    AdminConfiguration cfg = configureNub( (AdminConfiguration) cmd.getConfigurationObject());
    cfg.operation = AdminOperation.MATCH;
    //cfg.key = UUID.fromString("61a5f178-b5fb-4484-b6d8-9b129739e59d");
    cfg.key = Constants.COL_DATASET_KEY;
    cmd.doRun();
  }
  
  @Test
  public void runUat() throws Exception {
    AdminCommand cmd = new AdminCommand();
    AdminConfiguration cfg = configureUat( (AdminConfiguration) cmd.getConfigurationObject());
    cfg.operation = AdminOperation.CLEAN_ORPHANS;
    cmd.doRun();
  }

  @Test
  public void runLocalCrawl() throws Exception {
    AdminCommand cmd = new AdminCommand();
    AdminConfiguration cfg = configureLocal( (AdminConfiguration) cmd.getConfigurationObject());
    cfg.operation = AdminOperation.CRAWL;
    cfg.key = UUID.fromString("a739f783-08c1-4d47-a8cc-2e9e6e874202");
    cfg.registry.wsUrl = "/Users/markus/Desktop/clb/datasets.txt";
    cmd.doRun();
  }

  @Test
  public void runLocalDbUpdate() throws Exception {
    AdminCommand cmd = new AdminCommand();
    AdminConfiguration cfg = configureLocal( (AdminConfiguration) cmd.getConfigurationObject());
    cfg.operation = AdminOperation.SCHEMA_UPDATE;
    cmd.doRun();
  }

  @Test
  public void runLocalExport() throws Exception {
    AdminCommand cmd = new AdminCommand();
    AdminConfiguration cfg = configureLocal( (AdminConfiguration) cmd.getConfigurationObject());
    cfg.operation = AdminOperation.EXPORT;
    cfg.key = Constants.NUB_DATASET_KEY;
    //cfg.key = UUID.fromString("f5015bc0-b466-4f0e-ad00-16b59f156471");
    cmd.doRun();
  }

}