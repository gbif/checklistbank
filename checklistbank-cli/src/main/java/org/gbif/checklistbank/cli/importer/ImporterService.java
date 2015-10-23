package org.gbif.checklistbank.cli.importer;

import org.gbif.api.model.crawler.FinishReason;
import org.gbif.api.model.crawler.ProcessState;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.cli.common.RabbitDatasetService;
import org.gbif.checklistbank.cli.common.ZookeeperUtils;
import org.gbif.checklistbank.cli.deletion.DeleteService;
import org.gbif.checklistbank.index.NameUsageIndexService;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.DatasetImportServiceMyBatis;
import org.gbif.common.messaging.api.messages.ChecklistNormalizedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImporterService extends RabbitDatasetService<ChecklistNormalizedMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(ImporterService.class);

    private final ImporterConfiguration cfg;
    private DatasetImportServiceCombined importService;
    private NameUsageService nameUsageService;
    private UsageService usageService;
    private final ZookeeperUtils zkUtils;

    public ImporterService(ImporterConfiguration cfg) {
        super("clb-importer", cfg.poolSize, cfg.messaging, cfg.ganglia, "import");
        this.cfg = cfg;
        try {
            zkUtils = new ZookeeperUtils(cfg.zookeeper.getCuratorFramework());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // init mybatis layer and solr once from cfg instance
        Injector inj = Guice.createInjector(cfg.clb.createServiceModule(), new RealTimeModule(cfg.solr));
        initDbPool(inj);
        importService = new DatasetImportServiceCombined((DatasetImportServiceMyBatis) inj.getInstance(DatasetImportService.class), inj.getInstance(NameUsageIndexService.class), cfg.poolSize);
        nameUsageService = inj.getInstance(NameUsageService.class);
        usageService = inj.getInstance(UsageService.class);
    }

    @Override
    protected void process(ChecklistNormalizedMessage msg) throws Exception {
        try {
            Importer importer = Importer.create(cfg, msg.getDatasetUuid(), registry, importService, nameUsageService, usageService);
            importer.run();
            // notify rabbit
            Date crawlFinished = zkUtils.getDate(msg.getDatasetUuid(), ZookeeperUtils.FINISHED_CRAWLING);
            if (crawlFinished == null) {
                LOG.warn("No crawlFinished date found in zookeeper, use current date instead for dataset {}", msg.getDatasetUuid());
                crawlFinished = new Date();
            }
            send(new ChecklistSyncedMessage(msg.getDatasetUuid(), crawlFinished, importer.getSyncCounter(), importer.getDelCounter()));
            // finally delete artifacts unless configured not to
            if (cfg.deleteNeo) {
                DeleteService.deleteStorageFiles(cfg.neo, msg.getDatasetUuid());
            }

        } finally {
            zkUtils.createOrUpdate(msg.getDatasetUuid(), ZookeeperUtils.PROCESS_STATE_CHECKLIST, ProcessState.FINISHED);
        }
    }

    @Override
    protected void failed(UUID datasetKey) {
        zkUtils.createOrUpdate(datasetKey, ZookeeperUtils.FINISHED_REASON, FinishReason.ABORT);
    }

    @Override
    @VisibleForTesting
    protected void startUp() throws Exception {
        super.startUp();
    }

    @Override
    protected void shutDown() throws Exception {
        importService.close();
        super.shutDown();
    }

    @Override
    public Class<ChecklistNormalizedMessage> getMessageClass() {
        return ChecklistNormalizedMessage.class;
    }

}
