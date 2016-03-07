package org.gbif.checklistbank.cli.analysis;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.checklistbank.cli.common.RabbitDatasetService;
import org.gbif.checklistbank.service.DatasetAnalysisService;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.common.messaging.api.messages.BackboneChangedMessage;
import org.gbif.common.messaging.api.messages.ChecklistAnalyzedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;

import java.io.IOException;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalysisService extends RabbitDatasetService<ChecklistSyncedMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(AnalysisService.class);

    private final DatasetAnalysisService analysisService;

    public AnalysisService(AnalysisConfiguration cfg) {
        super("clb-analysis", cfg.poolSize, cfg.messaging, cfg.ganglia, "analyze");
        Injector inj = Guice.createInjector(ChecklistBankServiceMyBatisModule.create(cfg.clb));
        analysisService = inj.getInstance(DatasetAnalysisService.class);
        hds = (HikariDataSource) inj.getInstance(InternalChecklistBankServiceMyBatisModule.DATASOURCE_KEY);
    }

    @Override
    public Class<ChecklistSyncedMessage> getMessageClass() {
        return ChecklistSyncedMessage.class;
    }

    @Override
    protected void process(ChecklistSyncedMessage msg) throws IOException {
        DatasetMetrics metrics = analysisService.analyse(msg.getDatasetUuid(), msg.getCrawlFinished());
        send(new ChecklistAnalyzedMessage(msg.getDatasetUuid()));
        if (Constants.NUB_DATASET_KEY.equals(msg.getDatasetUuid())) {
            send(new BackboneChangedMessage(metrics));
        }
    }

}
