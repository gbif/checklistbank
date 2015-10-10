package org.gbif.checklistbank.cli.tree;

import org.gbif.checklistbank.neo.UsageDao;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;

import org.kohsuke.MetaInfServices;
import org.neo4j.graphdb.Transaction;

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
                NubUsage nub = dao.readNub(dao.getNeo().getNodeById(cfg.key));
                System.out.println(nub);
            }
        }

        try (Transaction tx = dao.beginTx()) {
            dao.printTree(System.out);
        } finally {
            dao.close();
        }
    }

}
