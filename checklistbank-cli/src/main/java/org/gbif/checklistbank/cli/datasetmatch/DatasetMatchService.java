package org.gbif.checklistbank.cli.datasetmatch;

import org.gbif.checklistbank.cli.common.RabbitDatasetService;
import org.gbif.checklistbank.nub.lookup.DatasetMatchFailed;
import org.gbif.checklistbank.nub.lookup.IdLookup;
import org.gbif.checklistbank.nub.lookup.NubMatchService;
import org.gbif.checklistbank.nub.lookup.ReloadingIdLookup;
import org.gbif.checklistbank.service.DatasetImportService;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.yammer.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetMatchService extends RabbitDatasetService<MatchDatasetMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetMatchService.class);

  private final NubMatchService matcher;
  private static final String QUEUE = "clb-dataset-matcher";
  private final DatasetImportService importService;
  private final Timer timer = registry.timer("nub matcher process time");

  public DatasetMatchService(DatasetMatchConfiguration cfg) {
    super(QUEUE, cfg.poolSize, cfg.messaging, cfg.ganglia, "match");

    try {
      Injector clbInj = Guice.createInjector(cfg.clb.createServiceModule());
      importService = clbInj.getInstance(DatasetImportService.class);
      // loads all nub usages directly from clb postgres - this can take a few minutes
      // use the reloading version that listens to nub changed messages and reinits the data itself
      IdLookup lookup = new ReloadingIdLookup(cfg.clb, listener, QUEUE);
      matcher = new NubMatchService(cfg.clb, lookup, importService, publisher);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Class<MatchDatasetMessage> getMessageClass() {
    return MatchDatasetMessage.class;
  }

  @Override
  protected void process(MatchDatasetMessage msg) throws Exception {
    final Timer.Context context = timer.time();
    try {
      matcher.matchDataset(msg.getDatasetUuid());

    } catch (DatasetMatchFailed e) {
      LOG.error("Dataset matching failed for {}", msg.getDatasetUuid(), e);
    }
    context.close();
  }

  @Override
  protected void shutDown() throws Exception {
    super.shutDown();
    importService.close();
  }
}
