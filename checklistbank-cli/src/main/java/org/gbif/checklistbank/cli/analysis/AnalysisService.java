package org.gbif.checklistbank.cli.analysis;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.cli.common.RabbitBaseService;
import org.gbif.checklistbank.cli.deletion.DeleteService;
import org.gbif.checklistbank.service.DatasetAnalysisService;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.common.messaging.api.messages.BackboneChangedMessage;
import org.gbif.common.messaging.api.messages.ChecklistAnalyzedMessage;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;

import java.io.IOException;
import javax.sql.DataSource;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalysisService extends RabbitBaseService<ChecklistSyncedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(AnalysisService.class);

  private final DatasetAnalysisService analysisService;

  public AnalysisService(AnalysisConfiguration cfg) {
    super("clb-analysis", cfg.poolSize, cfg.messaging, cfg.ganglia);
    Injector inj = Guice.createInjector(cfg.clb.createServiceModule());
    analysisService = inj.getInstance(DatasetAnalysisService.class);
    Key<DataSource> dsKey = Key.get(DataSource.class, Names.named(InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME));
    hds = (HikariDataSource) inj.getInstance(dsKey);
  }

  @Override
  public Class<ChecklistSyncedMessage> getMessageClass() {
    return ChecklistSyncedMessage.class;
  }

  @Override
  protected void process(ChecklistSyncedMessage msg) throws IOException {
    analysisService.analyse(msg.getDatasetUuid(), msg.getCrawlFinished());
    send(new ChecklistAnalyzedMessage(msg.getDatasetUuid()));
    if (Constants.NUB_DATASET_KEY.equals(msg.getDatasetUuid())) {
      send(new BackboneChangedMessage());
    }
  }
}
