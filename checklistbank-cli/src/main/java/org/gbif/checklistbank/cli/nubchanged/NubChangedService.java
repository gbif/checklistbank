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
import org.gbif.checklistbank.nub.ParentStack;
import org.gbif.checklistbank.nub.lookup.IdLookup;
import org.gbif.checklistbank.nub.lookup.IdLookupImpl;
import org.gbif.checklistbank.nub.lookup.LookupUsage;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.checklistbank.nub.source.ClbUsageIteratorNeo;
import org.gbif.checklistbank.service.DatasetImportService;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NubChangedService extends AbstractIdleService implements MessageCallback<BackboneChangedMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(NubChangedService.class);

    public static final String QUEUE = "clb-matcher";

    public static final String MATCH_METER = "taxon.match";

    private final NubChangedConfiguration cfg;
    private MessageListener listener;
    private MessagePublisher publisher;
    private IdLookup nubLookup;
    private final DatasetImportService importService;
    private final DatasetService datasetService;
    private final NetworkService networkService;
    private final MetricRegistry registry = new MetricRegistry("matcher");
    private final Timer timer = registry.timer("nub matcher process time");

    public NubChangedService(NubChangedConfiguration configuration) {
        this.cfg = configuration;
        registry.meter(MATCH_METER);

        Injector regInj = cfg.registry.createRegistryInjector();
        datasetService = regInj.getInstance(DatasetService.class);
        networkService = regInj.getInstance(NetworkService.class);

        Injector clbInj = Guice.createInjector(cfg.clb.createServiceModule());
        importService = clbInj.getInstance(DatasetImportService.class);
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

        int counter = 0;
        for (Dataset d : Iterables.datasets(DatasetType.CHECKLIST, datasetService)) {
            try {
                updateDataset(d);
                counter++;
            } catch (Exception e) {
                LOG.error("Failed to rematch checklist {} {}", d.getKey(), d.getTitle());
            }
        }
        context.stop();
        LOG.info("Updated all nub relations for all {} checklists", counter);
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

        // build new description reusing the existing intro and then list the current sources
        StringBuilder description = new StringBuilder();
        // remove existing source list
        Pattern SOURCE_LIST_PATTERN = Pattern.compile("\\n*The following sources from the.+$");
        description.append(SOURCE_LIST_PATTERN.matcher(nub.getDescription()).replaceAll(""));
        // append new source list
        description.append("\n\nThe following sources from the " +
                "<a href='http://www.gbif.org/network/"+Constants.NUB_NETWORK_KEY+"'>GBIF Backbone network</a> " +
                "have been used to assemble the GBIF backbone:\n");
        description.append("<u>");
        for (Map.Entry<UUID, Integer> src : msg.getMetrics().getCountByConstituent().entrySet()) {
            Dataset d = datasetService.get(src.getKey());
            description.append("<li>" + src.getValue() + " names from "+ d.getTitle() +"</li>");
        }
        description.append("</u>");
        nub.setDescription(description.toString());

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
    private void updateDataset(Dataset d) throws Exception {
        LOG.info("Rematch checklist {} {} to changed backbone", d.getKey(), d.getTitle());
        Map<Integer, Integer> relations = Maps.newHashMap();
        try (ClbUsageIteratorNeo iter = new ClbUsageIteratorNeo(cfg.clb, d.getKey(), d.getTitle())) {
            NubUsage unknown = new NubUsage();
            unknown.usageKey = Kingdom.INCERTAE_SEDIS.nubUsageID();
            unknown.kingdom = Kingdom.INCERTAE_SEDIS;
            // this is a taxonomically sorted iteration. We remember the parent kingdom using the ParentStack
            ParentStack parents = new ParentStack(unknown);
            for (SrcUsage u : iter) {
                parents.add(u);
                LookupUsage match = nubLookup.match(u.parsedName.canonicalName(), u.parsedName.getAuthorship(), u.parsedName.getYear(), u.rank, parents.nubKingdom());
                if (match != null) {
                    // add to relations
                    relations.put(u.key, match.getKey());
                    // store current kingdom in parent stack for further nub lookups of children
                    NubUsage nub = new NubUsage();
                    nub.kingdom = match.getKingdom();
                    parents.put(nub);
                } else {
                    // also store no matches as nulls so we can flag an issue
                    relations.put(u.key, null);
                }
            }
            LOG.info("Updating {} nub relations for dataset {}", relations.size(), d.getKey());
            importService.insertNubRelations(d.getKey(), relations);
        }
    }

    @Override
    public Class<BackboneChangedMessage> getMessageClass() {
        return BackboneChangedMessage.class;
    }
}
