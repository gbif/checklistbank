package org.gbif.checklistbank.cli.nubchanged;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.common.InterpretedEnum;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.eml.TaxonomicCoverage;
import org.gbif.api.model.registry.eml.TaxonomicCoverages;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.registry.metadata.EMLWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class BackboneDatasetUpdater {
  private static final Logger LOG = LoggerFactory.getLogger(BackboneDatasetUpdater.class);
  private final DatasetService datasetService;
  private final OrganizationService organizationService;
  private final NetworkService networkService;
  @VisibleForTesting
  protected static final Pattern SOURCE_LIST_PATTERN = Pattern.compile("\\s*The following +\\d* *sources from the.+$", Pattern.DOTALL);

  public BackboneDatasetUpdater(DatasetService datasetService, OrganizationService organizationService, NetworkService networkService) {
    this.datasetService = datasetService;
    this.organizationService = organizationService;
    this.networkService = networkService;
  }

  static class NubConstituent implements Comparable<NubConstituent> {
    public NubConstituent(UUID key, String title, int count) {
      this.key = key;
      this.title = title;
      this.count = count;
    }

    public final UUID key;
    public final String title;
    public final int count;

    @Override
    public int compareTo(NubConstituent o) {
      return ComparisonChain.start()
          .compare(count, o.count, Ordering.natural().reverse())
          .compare(title, o.title)
          .result();
    }
  }

  public Dataset updateBackboneDataset(DatasetMetrics metrics) {
    LOG.info("Updating backbone dataset metadata");

    // update existing metadata (all fixed metadata is curated manually in the registry
    // lets load it first
    Dataset nub = datasetService.get(Constants.NUB_DATASET_KEY);
    nub.setPubDate(metrics.getCreated());
    List<TaxonomicCoverage> taxa = Lists.<TaxonomicCoverage>newArrayList();
    for (Kingdom k : Kingdom.values()) {
      new TaxonomicCoverage(k.scientificName(), null, new InterpretedEnum<String, Rank>("Kingdom", Rank.KINGDOM));
    }
    nub.setTaxonomicCoverages(Lists.newArrayList(new TaxonomicCoverages("All life", taxa)));
    nub.setRights("CC0 1.0");

    int plaziCounts = 0;
    List<NubConstituent> constituents = Lists.newArrayList();
    for (Map.Entry<UUID, Integer> src : metrics.getCountByConstituent().entrySet()) {
      Dataset d = datasetService.get(src.getKey());
      if (d.getPublishingOrganizationKey().equals(Constants.PLAZI_ORG_KEY)) {
        plaziCounts += src.getValue();
      } else {
        constituents.add(new NubConstituent(src.getKey(), d.getTitle(), src.getValue()));
      }
    }
    // add plazi
    if (plaziCounts > 0) {
      Organization plazi = organizationService.get(Constants.PLAZI_ORG_KEY);
      constituents.add(new NubConstituent(Constants.PLAZI_ORG_KEY, plazi.getTitle(), plaziCounts));
    }
    // sort constituents by number of names
    Collections.sort(constituents);

    // build new description reusing the existing intro and then list the current sources
    StringBuilder description = new StringBuilder();

    // remove existing source list
    description.append(SOURCE_LIST_PATTERN.matcher(nub.getDescription()).replaceAll(""));

    // append new source list
    description.append("\n\nThe following " + constituents.size() + " sources from the " +
        "<a href='http://www.gbif.org/network/" + Constants.NUB_NETWORK_KEY + "'>GBIF Backbone network</a> " +
        "have been used to assemble the GBIF backbone:\n");
    description.append("<ul>");
    for (NubConstituent nc : constituents) {
      description.append("<li>" + nc.title+ " - " + nc.count+ " names</li>");
    }
    description.append("</ul>");
    nub.setDescription(description.toString());

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
    Set<UUID> constituentKeys = Sets.newHashSet(metrics.getCountByConstituent().keySet());
    LOG.info("Updating backbone source network with {} constituents", constituentKeys.size());
    for (Dataset d : Iterables.networkDatasets(Constants.NUB_NETWORK_KEY, null, networkService)) {
      if (!constituentKeys.remove(d.getKey())) {
        LOG.debug("Remove backbone source network constituent {} {}", d.getKey(), d.getTitle());
        networkService.removeConstituent(Constants.NUB_NETWORK_KEY, d.getKey());
      }
    }
    // now add the remaining ones
    for (UUID datasetKey : constituentKeys) {
      LOG.debug("Add new backbone source network constituent {}", datasetKey);
      networkService.addConstituent(Constants.NUB_NETWORK_KEY, datasetKey);
    }

    return nub;
  }
}
