package org.gbif.checklistbank.cli.matcher;

import org.gbif.checklistbank.cli.common.RabbitDatasetService;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.index.guice.Solr;
import org.gbif.checklistbank.nub.lookup.DatasetMatchFailed;
import org.gbif.checklistbank.nub.lookup.IdLookup;
import org.gbif.checklistbank.nub.lookup.NubMatchService;
import org.gbif.checklistbank.nub.lookup.ReloadingIdLookup;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.checklistbank.service.mybatis.guice.Mybatis;
import org.gbif.common.messaging.api.messages.MatchDatasetMessage;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.yammer.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatcherService extends RabbitDatasetService<MatchDatasetMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(MatcherService.class);

  private NubMatchService matcher;
  private static final String QUEUE = "clb-matcher";
  private final DatasetImportService sqlImportService;
  private final DatasetImportService solrImportService;
  private final Timer timer = registry.timer("nub matcher process time");
  private final MatcherConfiguration cfg;

  public MatcherService(MatcherConfiguration cfg) {
    super(QUEUE, cfg.poolSize, cfg.messaging, cfg.ganglia, "match");
    this.cfg = cfg;
    Injector clbInj = Guice.createInjector(cfg.clb.createServiceModule(), new RealTimeModule(cfg.solr));
    sqlImportService = clbInj.getInstance(Key.get(DatasetImportService.class, Mybatis.class));
    solrImportService = clbInj.getInstance(Key.get(DatasetImportService.class, Solr.class));
  }

  @Override
  protected void startUpBeforeListening() throws Exception {
    // loads all nub usages directly from clb postgres - this can take a few minutes
    // use the reloading version that listens to nub changed messages and reinits the data itself
    IdLookup lookup = new ReloadingIdLookup(cfg.clb, listener, QUEUE);
    matcher = new NubMatchService(cfg.clb, lookup, sqlImportService, solrImportService, publisher);
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
    sqlImportService.close();
    solrImportService.close();
  }
}
