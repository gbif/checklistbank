package org.gbif.checklistbank.cli.nubchanged;

import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.BackboneChangedMessage;

import java.util.UUID;

import com.google.common.util.concurrent.AbstractIdleService;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatchService extends AbstractIdleService implements MessageCallback<BackboneChangedMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(MatchService.class);

  public static final String QUEUE = "clb-matcher";

  public static final String MATCH_METER = "taxon.match";

  private final MatchConfiguration cfg;
  private MessageListener listener;
  private MessagePublisher publisher;
  private final MetricRegistry registry = new MetricRegistry("matcher");
  private final Timer timer = registry.timer("nub matcher process time");

  public MatchService(MatchConfiguration configuration) {
    this.cfg = configuration;
    registry.meter(MATCH_METER);
  }

  @Override
  protected void startUp() throws Exception {
    cfg.ganglia.start(registry);

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
    final Timer.Context context = timer.time();
    try {
      LOG.info("Start rematching all checklists to changed backbone");
        //TODO: loop over all checklists but the nub and update each
        updateDataset(UUID.randomUUID());
    } finally {
      context.stop();
    }
  }

    /**
     * Updates a datasets nub matches.
     * Uses the internal Lookup to generate a complete id map and then does postgres writes in a separate thread ?!
     */
    private void updateDataset(UUID datasetKey) {

    }

  @Override
  public Class<BackboneChangedMessage> getMessageClass() {
    return BackboneChangedMessage.class;
  }
}
