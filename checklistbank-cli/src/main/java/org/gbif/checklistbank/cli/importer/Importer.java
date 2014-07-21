package org.gbif.checklistbank.cli.importer;

import com.yammer.metrics.Meter;
import com.yammer.metrics.MetricRegistry;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.checklistbank.cli.common.NeoRunnable;
import org.gbif.checklistbank.neo.traverse.TaxonomicNodeIterator;
import org.gbif.checklistbank.service.DatasetImportService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 *
 */
public class Importer extends NeoRunnable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Importer.class);

    private final ImporterConfiguration cfg;
    private final Meter syncMeter;
    private final DatasetImportService importService;

    public Importer(ImporterConfiguration cfg, UUID datasetKey, MetricRegistry registry) {
        super(datasetKey, cfg.neo, registry);
        this.cfg = cfg;
        this.syncMeter = registry.getMeters().get(ImporterService.SYNC_METER);
        //TODO: init mybatis layer from cfg instance
        importService = null;
    }

    public void run() {
        setupDb();
        syncDataset();
        tearDownDb();
        LOG.info("Importing of {} finished. Neo database shut down.", datasetKey);
    }

    private void syncDataset() {
        try (Transaction tx = db.beginTx()) {
            for (Node n : TaxonomicNodeIterator.all(db)) {
                // returns all accepted and synonyms
                NameUsage u = mapper.read(n);
                // TODO: deal with basionyms!!!
                importService.syncUsage(u);
                syncMeter.mark();
            }
        }
    }
}
