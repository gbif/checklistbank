package org.gbif.checklistbank.cli.analysis;

import com.google.common.base.Strings;
import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.checklistbank.cli.common.RabbitDatasetService;
import org.gbif.checklistbank.service.DatasetAnalysisService;
import org.gbif.checklistbank.service.mybatis.guice.ChecklistBankServiceMyBatisModule;
import org.gbif.common.messaging.api.messages.BackboneChangedMessage;
import org.gbif.common.messaging.api.messages.ChecklistAnalyzedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AnalysisService extends RabbitDatasetService<ChecklistSyncedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(AnalysisService.class);

  private final DatasetAnalysisService analysisService;
  private final DatasetIndexUpdater datasetIndexUpdater;


  public AnalysisService(AnalysisConfiguration cfg) {
    super("clb-analysis", cfg.poolSize, cfg.messaging, cfg.ganglia, "analyze", ChecklistBankServiceMyBatisModule.create(cfg.clb));
    analysisService = getInstance(DatasetAnalysisService.class);

    if (Strings.isNullOrEmpty(cfg.dataset.getServerHome())) {
      datasetIndexUpdater = null;
    } else {
      datasetIndexUpdater = new DatasetIndexUpdater(cfg.clb, cfg.dataset);
    }
  }

  @Override
  public Class<ChecklistSyncedMessage> getMessageClass() {
    return ChecklistSyncedMessage.class;
  }

  @Override
  protected void process(ChecklistSyncedMessage msg) throws IOException {
    DatasetMetrics metrics = analysisService.analyse(msg.getDatasetUuid(), msg.getCrawlFinished());
    if (datasetIndexUpdater != null) {
      datasetIndexUpdater.index(msg.getDatasetUuid());
    }

    send(new ChecklistAnalyzedMessage(msg.getDatasetUuid()));

    if (Constants.NUB_DATASET_KEY.equals(msg.getDatasetUuid())) {
      send(new BackboneChangedMessage(metrics));
    }
  }

  @Override
  protected void shutDown() throws Exception {
    if (datasetIndexUpdater != null) {
      datasetIndexUpdater.close();
    }
    super.shutDown();
  }
}
