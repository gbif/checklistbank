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
package org.gbif.checklistbank.cli.show;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.cli.BaseCommand;

import java.io.FileWriter;
import java.io.Writer;
import java.util.Collection;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

/**
 * Command that issues new normalize or import messages for manual admin purposes.
 */
public class ShowCommand extends BaseCommand {
    private final ShowConfiguration cfg = new ShowConfiguration();

    public ShowCommand() {
        super("show");
    }

    @Override
    protected Object getConfigurationObject() {
        return cfg;
    }

    @Override
    protected void doRun() {
        try {
          UsageDao dao = UsageDao.open(cfg.neo, cfg.key);
          Node root = null;
          try (Transaction tx = dao.beginTx()) {
            if (cfg.daoReport) {
              dao.consistencyNubReport();
              dao.logStats();

            } else {
              if (cfg.rootId != null || cfg.rootName != null) {
                if (cfg.rootId != null) {
                  System.out.println("Show root node " + cfg.rootId);
                  root = dao.getNeo().getNodeById(cfg.rootId);
                } else {
                  System.out.println("Show root node " + cfg.rootName);
                  Collection<Node> rootNodes = dao.findByName(cfg.rootName);
                  if (rootNodes.isEmpty()) {
                    System.out.println("No root found");
                    return;
                  } else if (rootNodes.size() > 1) {
                    System.out.println("Multiple root nodes found. Please select one by its id:");
                    for (Node n : rootNodes) {
                      System.out.println(n.getId() + ": " + n.getProperty(NeoProperties.SCIENTIFIC_NAME, "???"));
                    }
                    return;
                  }
                  root = rootNodes.iterator().next();
                }

                NubUsage nub = dao.readNub(root);
                System.out.println("NUB: " + (nub == null ? "null" : nub.toStringComplete()));

                NameUsage u = dao.readUsage(root, true);
                System.out.println("USAGE: " + u);
              }

              // show tree
              try (Writer writer = new FileWriter(cfg.file)) {
                dao.printTree(writer, cfg.format, cfg.fullNames, cfg.lowestRank, root);
              }
            }

          } finally {
            dao.close();
          }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
