package org.gbif.checklistbank.cli.deletion;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.RabbitBaseService;
import org.gbif.checklistbank.index.NameUsageIndexService;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.common.messaging.api.messages.RegistryChangeMessage;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.yammer.metrics.Timer;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteService extends RabbitBaseService<RegistryChangeMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteService.class);

    private final DeleteConfiguration cfg;
    private NameUsageIndexService solrService;
    private DatasetImportService mybatisService;
    private final Timer timerSolr = registry.timer(regName("solr.time"));
    private final Timer timerSql = registry.timer(regName("sql.time"));

    public DeleteService(DeleteConfiguration cfg) {
        super("clb-deletion", cfg.poolSize, cfg.messaging, cfg.ganglia);
        this.cfg = cfg;

        // init mybatis layer and solr from cfg instance
        Injector inj = Guice.createInjector(cfg.clb.createServiceModule(), new RealTimeModule(cfg.solr));
        initDbPool(inj);
        solrService = inj.getInstance(NameUsageIndexService.class);
        mybatisService = inj.getInstance(DatasetImportService.class);
    }

    /**
     * Deletes all neo and kvp files created during indexing.
     * @param cfg a neo configuration needed to point to the right setup
     * @param datasetKey the dataset to delete files for
     */
    public static void deleteStorageFiles(NeoConfiguration cfg, UUID datasetKey) {
        // delete neo & kvp storage files
        File kvp = cfg.kvp(datasetKey);
        if (kvp.exists() && !kvp.delete()) {
            LOG.warn("Failed to delete kvo data dir {}", kvp.getAbsoluteFile());
        }

        // delete neo storage files
        File neoDir = cfg.neoDir(datasetKey);
        if (neoDir.exists()) {
            try {
                FileUtils.deleteDirectory(neoDir);
            } catch (IOException e) {
                LOG.warn("Failed to delete neo data dir {}", neoDir.getAbsoluteFile());
            }
        }
        LOG.info("Deleted dataset storage files for {}", datasetKey);
    }

    protected void process(RegistryChangeMessage msg) throws RuntimeException {
        final UUID key = ((Dataset) msg.getOldObject()).getKey();
        LOG.info("Deleting data for checklist {}", key);
        // solr
        Timer.Context context = timerSolr.time();
        try {
            solrService.delete(key);
        } catch (Throwable e) {
            LOG.error("Failed to delete dataset [{}] from solr", key, e);
        } finally {
            context.stop();
        }

        // postgres usage
        context = timerSql.time();
        try {
            mybatisService.deleteDataset(key);
        } catch (Throwable e) {
            LOG.error("Failed to delete dataset [{}] from postgres", key, e);
        } finally {
            context.stop();
        }
        deleteStorageFiles(cfg.neo, key);
    }

    @Override
    public Class<RegistryChangeMessage> getMessageClass() {
        return RegistryChangeMessage.class;
    }

    @Override
    protected boolean ignore(RegistryChangeMessage msg) {
        if (RegistryChangeMessage.ChangeType.DELETED == msg.getChangeType() && Dataset.class.equals(msg.getObjectClass())) {
            Dataset d = (Dataset) msg.getOldObject();
            if (DatasetType.CHECKLIST == d.getType()) {
                return false;
            }
        }
        return true;
    }
}
