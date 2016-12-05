package org.gbif.checklistbank.cli.show;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.cli.model.GraphFormat;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;

import java.io.FileWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.UUID;

import com.google.common.base.Joiner;
import org.kohsuke.MetaInfServices;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

/**
 * Command that issues new normalize or import messages for manual admin purposes.
 */
@MetaInfServices(Command.class)
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
          UsageDao dao = UsageDao.persistentDao(cfg.neo, cfg.key, true, null, false);
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

    public void validate() {
      UsageDao dao = UsageDao.persistentDao(cfg.neo, UUID.fromString("UUID"), true, null, false);
      try (Transaction tx = dao.beginTx()) {
        // log some key dataset stats
        dao.logStats();

        // iterate over all records and check their issues
        Joiner join = Joiner.on("; ").skipNulls();
        for (Node n : dao.allNodes()) {
          NameUsage u = dao.readUsage(n, false);
          System.out.println(join.join(u.getIssues()));
        }

        // show tree
        try (Writer writer = new FileWriter(cfg.file)) {
          dao.printTree(writer, GraphFormat.TEXT);

        } catch (Exception e) {
          e.printStackTrace();
        }

      } finally {
        dao.close();
      }
    }

}
