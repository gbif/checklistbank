package org.gbif.checklistbank.cli.importer;

import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.cursors.IntIntCursor;
import com.yammer.metrics.Meter;
import com.yammer.metrics.MetricRegistry;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.cli.common.NeoRunnable;
import org.gbif.checklistbank.neo.traverse.TaxonomicNodeIterator;
import org.gbif.checklistbank.service.DatasetImportService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class Importer extends NeoRunnable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Importer.class);

    private final ImporterConfiguration cfg;
    private final Meter syncMeter;
    private final Meter basionymMeter;
    private final AtomicInteger failed = new AtomicInteger();
    private final DatasetImportService importService;
    // neo internal ids to clb usage keys
    private IntIntOpenHashMap clbKeys = new IntIntOpenHashMap();
    // map based around internal neo4j node ids:
    private IntIntOpenHashMap basionyms = new IntIntOpenHashMap();

    public Importer(ImporterConfiguration cfg, UUID datasetKey, MetricRegistry registry) {
        super(datasetKey, cfg.neo, registry);
        this.cfg = cfg;
        this.syncMeter = registry.getMeters().get(ImporterService.SYNC_METER);
        this.basionymMeter = registry.meter(ImporterService.SYNC_BASIONYM_METER);
        //TODO: init mybatis layer from cfg instance
        importService = null;
    }

    public void run() {
        setupDb();
        syncDataset();
        tearDownDb();
        LOG.info("Importing of {} finished. Neo database shut down.", datasetKey);
    }

    /**
     * Iterates over all accepted taxa in taxonomical order including all synonyms and syncs the usage individually
     * with Checklist Bank Postgres. As basionym relations can crosslink basically any record we first set the basionym
     * key to null and update just those keys in a second iteration. Most usages will not have a basionymKey, so
     * performance should only be badly impacted in rare cases.
     */
    private void syncDataset() {
        failed.set(0);
        try (Transaction tx = db.beginTx()) {
            for (Node n : TaxonomicNodeIterator.all(db)) {
                // returns all accepted and synonyms
                NameUsageContainer u = buildClbNameUsage(n);
                // remember basionymKey if we have not processed it before already
                if (u.getBasionymKey() != null && !clbKeys.containsKey(u.getBasionymKey())) {
                    // these are internal neo4j node ids!
                    basionyms.put(u.getKey(), u.getBasionymKey());
                    u.setBasionymKey(null);
                }
                try {
                    importService.syncUsage(u);
                    syncMeter.mark();
                } catch (Exception e) {
                    failed.getAndIncrement();
                    LOG.error("Failed to sync usage {} from dataset {}", u.getTaxonID(), datasetKey, e);
                }
            }
        }

        // fix basionyms
        if (!basionyms.isEmpty()) {
            for (IntIntCursor bas : basionyms) {
                try {
                    importService.updateBasionym(clbKey(bas.key), clbKey(bas.value));
                    basionymMeter.mark();
                } catch (Exception e) {
                    failed.getAndIncrement();
                    LOG.error("Failed to update basionym for usage {} from dataset {}", bas.key, datasetKey, e);
                }
            }
        }
    }

    private Integer clbKey(Integer nodeId) {
        if (nodeId != null) {
            try {
                return clbKeys.get(nodeId);
            } catch (Exception e) {
                LOG.error("NodeId not in CLB yet: " + nodeId);
                throw e;
            }
        }
        return null;
    }

    /**
     * Reads the full name usage from neo and udpates all foreign keys to use CLB usage keys.
     */
    private NameUsageContainer buildClbNameUsage(Node n) {
        // this is using neo4j internal node ids as keys:
        NameUsageContainer u = mapper.read(n);
        u.setParentKey(clbKey(u.getParentKey()));
        u.setAcceptedKey(clbKey(u.getAcceptedKey()));
        for (Rank r : Rank.DWC_RANKS) {
            ClassificationUtils.setHigherRankKey(u,r, clbKey(u.getHigherRankKey(r)) );
        }
        return u;
    }
}
