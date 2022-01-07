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
package org.gbif.checklistbank.nub;

import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.model.GraphFormat;
import org.gbif.checklistbank.neo.UsageDao;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.UUID;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManualDbDump {
  private static final Logger LOG = LoggerFactory.getLogger(ManualDbDump.class);

  private static NeoConfiguration neo() {
    NeoConfiguration cfg = new NeoConfiguration();
    cfg.neoRepository = new File("/Users/markus/Desktop/repo/neo");
    cfg.mappedMemory = 1024;
    return cfg;
  }

  private static void dump2file(NeoConfiguration cfg, UUID datasetKey, GraphFormat format) throws Exception {
    File f = new File("/Users/markus/Desktop/repo/tree-" + datasetKey + "." + format.suffix);
    LOG.info("Dump local neo db {} to {} file {}", datasetKey, format, f.getAbsolutePath());
    UsageDao dao = UsageDao.open(cfg, datasetKey);
    try (Transaction tx = dao.beginTx()) {
      dao.printTree(new BufferedWriter(new FileWriter(f)), GraphFormat.TAB);
    }
    dao.close();
    LOG.info("Neo dumped");
  }

  public static void main(String[] args) throws Exception {
    //ClbConfigurationUtils.uat()
    //dump2neo(neo(), uat(), UUID.fromString("99948a8b-63b2-41bf-9d10-6e007e967789"), false);
    dump2file(neo(), UUID.fromString("99948a8b-63b2-41bf-9d10-6e007e967789"), GraphFormat.TAB);
  }
}