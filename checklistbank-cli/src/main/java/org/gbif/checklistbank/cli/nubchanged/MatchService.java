package org.gbif.checklistbank.cli.nubchanged;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.nub.lookup.IdLookup;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.BackboneChangedMessage;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Injector;
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
    private IdLookup nubLookup;
    private final DatasetService datasetService;
    private final MetricRegistry registry = new MetricRegistry("matcher");
    private final Timer timer = registry.timer("nub matcher process time");

    public MatchService(MatchConfiguration configuration) {
        this.cfg = configuration;
        registry.meter(MATCH_METER);
        Injector regInj = cfg.registry.createRegistryInjector();
        datasetService = regInj.getInstance(DatasetService.class);
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
            nubLookup = new IdLookup(cfg.clb);
            PagingRequest req = new PagingRequest(0, 100);
            PagingResponse<Dataset> resp = null;
            while (resp == null || !resp.isEndOfRecords()) {
                resp = datasetService.listByType(DatasetType.CHECKLIST, req);
                for (Dataset d : resp.getResults()) {
                    updateDataset(d);
                }
                req.nextPage();
            }
        } catch (Exception e) {
            LOG.error("Failed to rematch checklists to new backbone");
        } finally {
            context.stop();
        }
    }

    /**
     * Updates a datasets nub matches.
     * Uses the internal Lookup to generate a complete id map and then does postgres writes in a separate thread ?!
     */
    private void updateDataset(Dataset d) {
        LOG.info("Rematch checklist {} {} to changed backbone", d.getKey(), d.getTitle());
        // TODO: use idlookup and write to pg...
    }

    @Override
    public Class<BackboneChangedMessage> getMessageClass() {
        return BackboneChangedMessage.class;
    }
}
