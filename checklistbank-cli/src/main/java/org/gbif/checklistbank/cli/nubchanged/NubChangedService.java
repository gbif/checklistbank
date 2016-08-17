package org.gbif.checklistbank.cli.nubchanged;

import org.gbif.api.model.Constants;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.cli.exporter.Exporter;
import org.gbif.checklistbank.config.MetricModule;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.BackboneChangedMessage;
import org.gbif.common.messaging.api.messages.MatchDatasetMessage;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NubChangedService extends AbstractIdleService implements MessageCallback<BackboneChangedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(NubChangedService.class);

  public static final String QUEUE = "clb-matcher";

  public static final String MATCH_METER = "taxon.match";

  private final NubChangedConfiguration cfg;
  private MessageListener listener;
  private MessagePublisher publisher;
  private final DatasetService datasetService;
  private final BackboneDatasetUpdater nubUpdService;
  private final MetricRegistry registry;

  public NubChangedService(NubChangedConfiguration configuration) {
    this.cfg = configuration;
    Injector regInj = cfg.registry.createRegistryInjector(new MetricModule(cfg.ganglia));
    datasetService = regInj.getInstance(DatasetService.class);
    nubUpdService = new BackboneDatasetUpdater(datasetService, regInj.getInstance(OrganizationService.class), regInj.getInstance(NetworkService.class));
    registry = regInj.getInstance(MetricRegistry.class);
    registry.meter(MATCH_METER);
  }

  @Override
  protected void startUp() throws Exception {
    publisher = new DefaultMessagePublisher(cfg.messaging.getConnectionParameters());

    listener = new MessageListener(cfg.messaging.getConnectionParameters(), 1);
    listener.listen(QUEUE, 1, this);
  }

  @Override
  protected void shutDown() throws Exception {
    if (listener != null) {
      listener.close();
    }
    if (publisher != null) {
      publisher.close();
    }
  }

  @Override
  public void handleMessage(BackboneChangedMessage msg) {
    Dataset nub = nubUpdService.updateBackboneDataset(msg.getMetrics());

    // now export the dataset?
    if (cfg.exportNub) {
      Exporter exporter = Exporter.create(cfg.exportRepository, cfg.clb, cfg.registry.wsUrl);
      exporter.export(nub);
    }

    if (cfg.rematchChecklists) {
      rematchChecklists();
    }
  }

  private void rematchChecklists() {
    try {
      LOG.info("Start sending match dataset messages for all checklists, starting with CoL");

      int counter = 1;
      // make sure we match CoL first as we need that to anaylze datasets (nub & col overlap of names)
      publisher.send(new MatchDatasetMessage(Constants.COL_DATASET_KEY));
      for (Dataset d : Iterables.datasets(DatasetType.CHECKLIST, datasetService)) {
        if (Constants.COL_DATASET_KEY.equals(d.getKey())) {
          continue;
        }
        publisher.send(new MatchDatasetMessage(d.getKey()));
        counter++;
      }
      LOG.info("Send dataset match message for all {} checklists", counter);

    } catch (Exception e) {
      LOG.error("Failed to handle BackboneChangedMessage", e);
    }
  }

  @Override
  public Class<BackboneChangedMessage> getMessageClass() {
    return BackboneChangedMessage.class;
  }
}
