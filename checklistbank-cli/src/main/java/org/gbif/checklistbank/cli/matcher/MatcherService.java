package org.gbif.checklistbank.cli.matcher;

import org.gbif.checklistbank.cli.common.RabbitDatasetService;
import org.gbif.checklistbank.cli.common.SpringContextBuilder;
import org.gbif.checklistbank.index.guice.RealTimeModule;
import org.gbif.checklistbank.index.guice.Solr;
import org.gbif.checklistbank.nub.lookup.DatasetMatchSummary;
import org.gbif.checklistbank.nub.lookup.NubMatchService;
import org.gbif.checklistbank.service.DatasetImportService;
import org.gbif.common.messaging.api.messages.ChecklistSyncedMessage;
import org.gbif.common.messaging.api.messages.MatchDatasetMessage;
import org.gbif.nub.lookup.straight.DatasetMatchFailed;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;

import java.util.Date;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class MatcherService extends RabbitDatasetService<MatchDatasetMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(MatcherService.class);

  // TODO: 05/01/2022 initialize context, startUp is in top class, RabbitBaseService
  private ApplicationContext ctx;

  private NubMatchService matcher;
  private static final String QUEUE = "clb-matcher";
  // TODO: 05/01/2022 initialize these
  private DatasetImportService sqlImportService;
  private DatasetImportService solrImportService;
  private final MatcherConfiguration cfg;
  private Timer timer;

  public MatcherService(MatcherConfiguration cfg) {
    super(QUEUE, cfg.poolSize, cfg.messaging, cfg.ganglia, "match");
    this.cfg = cfg;
  }

  private void initContext() {
    // TODO: 05/01/2022 configure
    ctx = SpringContextBuilder.create()
        .build();
  }

  @Override
  protected void initMetrics(MetricRegistry registry) {
    super.initMetrics(registry);
    timer = registry.timer("nub matcher process time");
  }

  @Override
  protected void startUpBeforeListening() throws Exception {
    // loads all nub usages directly from clb postgres - this can take a few minutes
    IdLookup lookup = IdLookupImpl.temp().load(cfg.clb, false);
    matcher = new NubMatchService(cfg.clb, cfg.neo, lookup, sqlImportService, solrImportService);
  }

  @Override
  public Class<MatchDatasetMessage> getMessageClass() {
    return MatchDatasetMessage.class;
  }

  @Override
  protected void process(MatchDatasetMessage msg) throws Exception {
    final Timer.Context context = timer.time();
    try {
      LOG.info("Start matching dataset {}", msg.getDatasetUuid());
      DatasetMatchSummary summary = matcher.matchDataset(msg.getDatasetUuid());
      LOG.info("Dataset {} matched sucessfully: {}", msg.getDatasetUuid(), summary);
      // now also request new metrics from the analysis step
      //ChecklistSyncedMessage triggers a new dataset analysis
      send(new ChecklistSyncedMessage(msg.getDatasetUuid(), new Date(), 0, 0));

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
