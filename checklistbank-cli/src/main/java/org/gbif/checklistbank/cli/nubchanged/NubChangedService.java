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
import org.gbif.checklistbank.cli.exporter.Exporter;
import org.gbif.checklistbank.config.MetricModule;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.BackboneChangedMessage;
import org.gbif.common.messaging.api.messages.MatchDatasetMessage;
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
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Injector;
import com.codahale.metrics.MetricRegistry;
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
  private final NetworkService networkService;
  private final MetricRegistry registry;

  public NubChangedService(NubChangedConfiguration configuration) {
    this.cfg = configuration;
    Injector regInj = cfg.registry.createRegistryInjector(new MetricModule(cfg.ganglia));
    datasetService = regInj.getInstance(DatasetService.class);
    networkService = regInj.getInstance(NetworkService.class);
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
    updateBackboneDataset(msg);

    rematchChecklists();
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

  private void updateBackboneDataset(BackboneChangedMessage msg) {
    LOG.info("Updating backbone dataset metadata");

    // update existing metadata (all fixed metadata is curated manually in the registry
    // lets load it first
    Dataset nub = datasetService.get(Constants.NUB_DATASET_KEY);
    nub.setPubDate(new Date());
    List<TaxonomicCoverage> taxa = Lists.<TaxonomicCoverage>newArrayList();
    for (Kingdom k : Kingdom.values()) {
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
        "<a href='http://www.gbif.org/network/" + Constants.NUB_NETWORK_KEY + "'>GBIF Backbone network</a> " +
        "have been used to assemble the GBIF backbone:\n");
    description.append("<u>");
    for (Map.Entry<UUID, Integer> src : msg.getMetrics().getCountByConstituent().entrySet()) {
      Dataset d = datasetService.get(src.getKey());
      description.append("<li>" + src.getValue() + " names from " + d.getTitle() + "</li>");
    }
    description.append("</u>");
    nub.setDescription(description.toString());
    nub.setRights("CC0 1.0");

    // convert to EML and send to registry
    try {
      StringWriter writer = new StringWriter();
      EMLWriter.write(nub, writer);
      writer.close();
      InputStream stream = new ByteArrayInputStream(writer.getBuffer().toString().getBytes(Charsets.UTF_8));
      datasetService.insertMetadata(Constants.NUB_DATASET_KEY, stream);
    } catch (RuntimeException | IOException e) {
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

    // now export the dataset
    Exporter exporter = Exporter.create(cfg.exportRepository, cfg.clb, cfg.registry.wsUrl);
    exporter.export(nub);

  }

  @Override
  public Class<BackboneChangedMessage> getMessageClass() {
    return BackboneChangedMessage.class;
  }
}
