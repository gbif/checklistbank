/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.cli.nubbuild;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.common.InterpretedEnum;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Network;
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
import java.util.*;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

/**
 * Updates the Backbone dataset metadata in the registry.
 */
public class BackboneDatasetUpdater {
  private static final Logger LOG = LoggerFactory.getLogger(BackboneDatasetUpdater.class);
  private final DatasetService datasetService;
  private final OrganizationService organizationService;
  private final NetworkService networkService;
  @VisibleForTesting
  protected static final Pattern SOURCE_LIST_PATTERN = Pattern.compile("\\s*The following +\\d* *sources have been used .+$", Pattern.DOTALL);

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
    Preconditions.checkNotNull(metrics, "No metrics found");
    LOG.info("Updating backbone dataset metadata based on metrics from {}", metrics.getCreated());

    // update existing metadata (all fixed metadata is curated manually in the registry
    // lets load it first
    Dataset nub = datasetService.get(Constants.NUB_DATASET_KEY);
    nub.setPubDate(metrics.getCreated());
    List<TaxonomicCoverage> taxa = Lists.<TaxonomicCoverage>newArrayList();
    for (Kingdom k : Kingdom.values()) {
      new TaxonomicCoverage(k.scientificName(), null, new InterpretedEnum<String, Rank>("Kingdom", Rank.KINGDOM));
    }
    nub.setTaxonomicCoverages(Lists.newArrayList(new TaxonomicCoverages("All life", taxa)));

    LOG.info("Aggregating all Plazi datasets");
    int plaziDatasets = 0;
    int plaziCounts = 0;
    List<NubConstituent> constituents = Lists.newArrayList();
    for (Map.Entry<UUID, Integer> src : metrics.getCountByConstituent().entrySet()) {
      Dataset d = datasetService.get(src.getKey());
      if (d.getPublishingOrganizationKey().equals(Constants.PLAZI_ORG_KEY)) {
        plaziCounts += src.getValue();
        plaziDatasets += 1;
      } else {
        constituents.add(new NubConstituent(src.getKey(), d.getTitle(), src.getValue()));
      }
    }
    // add plazi
    LOG.info("Found {} datasets with {} records from Plazi", plaziDatasets, plaziCounts);
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

    description.append("\n\nThe following " + constituents.size() + " sources have been used to assemble the GBIF backbone " +
        "with number of names given in brackets:\n");
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
      LOG.info("Updated backbone metadata successfully");
    } catch (RuntimeException | IOException e) {
      LOG.error("Failed to update backbone dataset metadata", e);
    }

    // update backbone sources network
    Network network = networkService.get(Constants.NUB_NETWORK_KEY);
    if (network == null) {
      LOG.warn("Backbone source network {} is missing in the registry", Constants.NUB_NETWORK_KEY);

    } else {
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
    }

    return nub;
  }
}
