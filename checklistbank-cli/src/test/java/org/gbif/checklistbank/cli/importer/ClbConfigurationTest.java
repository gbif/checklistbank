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
package org.gbif.checklistbank.cli.importer;

import org.gbif.checklistbank.config.ClbConfiguration;

import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClbConfigurationTest {

  @Test
  public void testPropTrip() throws Exception {
    ClbConfiguration cfg = new ClbConfiguration();
    cfg.databaseName = "test1";
    cfg.user = "test3";
    cfg.password = "test4";
    cfg.maximumPoolSize = 11;
    cfg.minimumIdle = 99;
    cfg.maxLifetime = 8877;
    cfg.idleTimeout = 20200;
    cfg.connectionInitSql = "SET work_mem='64MB'";
    cfg.connectionTimeout = 7777;
    cfg.parserTimeout = 1000;
    cfg.syncThreads = 3;

    Properties p = cfg.toProps(true);

    ClbConfiguration cfg2 = ClbConfiguration.fromProperties(p);

    assertEquals(cfg2, cfg);
  }
}
