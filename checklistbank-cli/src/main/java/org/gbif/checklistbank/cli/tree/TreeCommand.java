package org.gbif.checklistbank.cli.tree;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;

import org.kohsuke.MetaInfServices;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

/**
 * Simple command that reads an existing neo/kvp store and prints the entire tree
 * or optionally first a requested node by its id.
 */
@MetaInfServices(Command.class)
public class TreeCommand extends BaseCommand {
    private final ReaderConfiguration cfg = new ReaderConfiguration();

    public TreeCommand() {
        super("tree");
    }

    @Override
    protected Object getConfigurationObject() {
        return cfg;
    }

    @Override
    protected void doRun() {
        UsageDao dao = UsageDao.persistentDao(cfg.neo, cfg.datasetKey, true, null, false);

        if (cfg.key != null) {
            try (Transaction tx = dao.beginTx()) {
                Node n = dao.getNeo().getNodeById(cfg.key);

                NubUsage nub = dao.readNub(n);
                System.out.println("NUB: " + nub);

                NameUsage u = dao.readUsage(n, true);
                System.out.println("USAGE: " + u);
            }
        }

        // report stats
        try (Transaction tx = dao.beginTx()) {
            dao.logStats();
        }

        try (Transaction tx = dao.beginTx()) {
            dao.printTree();
        } finally {
            dao.close();
        }
    }

}
