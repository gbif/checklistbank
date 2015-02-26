package org.gbif.checklistbank.cli.nubupdate;

import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.BackboneChangedMessage;

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
    listener.listen(QUEUE, cfg.messaging.poolSize, this);
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
      LOG.warn("Handling of BackboneChangedMessage not implemented yet");

    } finally {
      context.stop();
    }
  }

  @Override
  public Class<BackboneChangedMessage> getMessageClass() {
    return BackboneChangedMessage.class;
  }
}
