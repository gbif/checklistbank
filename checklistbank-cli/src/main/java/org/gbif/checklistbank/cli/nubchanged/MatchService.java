package org.gbif.checklistbank.cli.nubchanged;

import org.gbif.api.model.Constants;
import org.gbif.api.model.common.InterpretedEnum;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.eml.TaxonomicCoverage;
import org.gbif.api.model.registry.eml.TaxonomicCoverages;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.nub.lookup.IdLookup;
import org.gbif.checklistbank.nub.lookup.IdLookupImpl;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.BackboneChangedMessage;
import org.gbif.registry.metadata.EMLWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
    private final NetworkService networkService;
    private final MetricRegistry registry = new MetricRegistry("matcher");
    private final Timer timer = registry.timer("nub matcher process time");

    public MatchService(MatchConfiguration configuration) {
        this.cfg = configuration;
        registry.meter(MATCH_METER);
        Injector regInj = cfg.registry.createRegistryInjector();
        datasetService = regInj.getInstance(DatasetService.class);
        networkService = regInj.getInstance(NetworkService.class);
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
        updateBackboneDataset(msg);

        final Timer.Context context = timer.time();
        try {
            LOG.info("Start rematching all checklists to changed backbone");
            nubLookup = new IdLookupImpl(cfg.clb);
        } catch (Exception e) {
            LOG.error("Failed to load backbone IdLookup", e);
        }

        for (Dataset d : Iterables.datasets(DatasetType.CHECKLIST, datasetService)) {
            try {
                updateDataset(d);
            } catch (Exception e) {
                LOG.error("Failed to rematch checklist {} {}", d.getKey(), d.getTitle());
            }
        }
        context.stop();
    }

    private void updateBackboneDataset(BackboneChangedMessage msg) {
        LOG.info("Updating backbone dataset metadata");

        // update existing metadata (all fixed metadata is curated manually in the registry
        // lets load it first
        Dataset nub = datasetService.get(Constants.NUB_DATASET_KEY);
        nub.setPubDate(new Date());
        List<TaxonomicCoverage> taxa = Lists.<TaxonomicCoverage>newArrayList();
        for (Kingdom k :Kingdom.values()) {
            new TaxonomicCoverage(k.scientificName(), null, new InterpretedEnum<String, Rank>("Kingdom", Rank.KINGDOM));
        }
        nub.setTaxonomicCoverages(Lists.newArrayList(new TaxonomicCoverages("All life", taxa)));
        // convert to EML and send to registry
        try {
            StringWriter writer = new StringWriter();
            EMLWriter.write(nub, writer);
            writer.close();
            InputStream stream = new ByteArrayInputStream(writer.getBuffer().toString().getBytes(Charsets.UTF_8));
            datasetService.insertMetadata(Constants.NUB_DATASET_KEY, stream);
        } catch (IOException e) {
            LOG.error("Failed to update backbone dataset metadata", e);
        }

        // update backbone sources network
        Set<UUID> constituents = Sets.newHashSet(msg.getMetrics().getCountByConstituent().keySet());
        for (Dataset d : Iterables.networkDatasets(Constants.NUB_NETWORK_KEY, null, networkService)) {
            if (!constituents.remove(d.getKey())) {
                LOG.debug("Remove backbone source network constituent {} {}", d.getKey(), d.getTitle());
                networkService.removeConstituent(Constants.NUB_NETWORK_KEY, d.getKey());
            }
        }
        // now add the remaining ones
        for (UUID datasetKey : constituents) {
            LOG.debug("Add new backbone source network constituent {}", datasetKey);
            networkService.addConstituent(Constants.NUB_NETWORK_KEY, datasetKey);
        }

        // TODO: update dwca download file
        // http://dev.gbif.org/issues/browse/POR-2816
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
