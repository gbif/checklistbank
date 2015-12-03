package org.gbif.checklistbank.cli.show;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.checklistbank.neo.NeoProperties;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.neo.traverse.TreePrinter;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;

import java.io.FileWriter;
import java.io.Writer;

import org.kohsuke.MetaInfServices;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command that issues new normalize or import messages for manual admin purposes.
 */
@MetaInfServices(Command.class)
public class ShowCommand extends BaseCommand {
    private static final Logger LOG = LoggerFactory.getLogger(ShowCommand.class);
    private static final String DWCA_SUFFIX = ".dwca";
    private final ShowConfiguration cfg = new ShowConfiguration();
    private DatasetService datasetService;

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
          try (Transaction tx = dao.beginTx()) {
            if (cfg.usageKey != null) {
              Node n = dao.getNeo().getNodeById(cfg.usageKey);

              NubUsage nub = dao.readNub(n);
              System.out.println("NUB: " + nub.toStringComplete());

              NameUsage u = dao.readUsage(n, true);
              System.out.println("USAGE: " + u);

            } else {
              // show entire tree
              dao.logStats();
              try (Writer writer = new FileWriter(cfg.file)) {
                if (cfg.canonical) {
                  dao.printTree(new TreePrinter(writer, NeoProperties.CANONICAL_NAME));
                } else {
                  dao.printTree(writer, cfg.xml);
                }
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
