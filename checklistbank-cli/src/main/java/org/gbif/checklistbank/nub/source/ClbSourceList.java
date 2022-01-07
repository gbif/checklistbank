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
package org.gbif.checklistbank.nub.source;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.checklistbank.cli.nubbuild.NubConfiguration;
import org.gbif.checklistbank.cli.nubbuild.NubSourceConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.gbif.registry.ws.client.DatasetClient;
import org.gbif.registry.ws.client.InstallationClient;
import org.gbif.registry.ws.client.OrganizationClient;
import org.gbif.ws.client.ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A source for nub sources backed by usage data from checklistbank.
 * The list of source datasets is discovered by reading a configured tab delimited online file.
 * The sources are then loaded asynchronously through a single background thread into temporary neo4j databases.
 */
public class ClbSourceList extends NubSourceList {

  private static final Logger LOG = LoggerFactory.getLogger(ClbSourceList.class);

  private final DatasetService datasetService;
  private final OrganizationService organizationService;
  private final InstallationService installationService;

  public static ClbSourceList create(NubConfiguration cfg) {
    ClientBuilder clientBuilder = new ClientBuilder();
    clientBuilder.withUrl(cfg.registry.wsUrl);
    if (cfg.registry.password != null && cfg.registry.user != null) {
      clientBuilder.withCredentials(cfg.registry.user, cfg.registry.password);
    }
    DatasetService datasetService = clientBuilder.build(DatasetClient.class);
    OrganizationService organizationService = clientBuilder.build(OrganizationClient.class);
    InstallationService installationService = clientBuilder.build(InstallationClient.class);

    return new ClbSourceList(datasetService, organizationService, installationService, cfg);
  }

  public static ClbSourceList create(NubConfiguration cfg, List<UUID> sourceDatasetKeys) {
    ClientBuilder clientBuilder = new ClientBuilder();
    clientBuilder.withUrl(cfg.registry.wsUrl);
    DatasetService datasetService = clientBuilder.build(DatasetClient.class);

    List<NubSource> sources = new ArrayList<>();
    for (UUID dKey : sourceDatasetKeys) {
      NubSourceConfig ns = new NubSourceConfig();
      ns.key = dKey;
      sources.add(buildSource(datasetService.get(dKey), cfg, ns));
    }
    return new ClbSourceList(cfg, sources);
  }

  public ClbSourceList(NubConfiguration cfg, List<NubSource> sources) {
    super(cfg);
    datasetService = null;
    organizationService = null;
    installationService = null;
    submitSources(sources);
  }

  public ClbSourceList(DatasetService datasetService, OrganizationService organizationService, InstallationService installationService, NubConfiguration cfg) {
    super(cfg);
    this.datasetService = datasetService;
    this.organizationService = organizationService;
    this.installationService = installationService;
    submitSources(loadSources());
  }

  private static NubSource buildSource(Dataset d, NubConfiguration cfg, NubSourceConfig sourceConfig) {
    NubSource src = new ClbSource(cfg.clb, cfg.neoSources, d.getKey(), d.getTitle(), sourceConfig.exclude);
    src.created = d.getCreated();
    src.ignoreSynonyms = !sourceConfig.synonyms;
    src.nameTypeMapping = sourceConfig.nameTypeMapping;
    src.nomenclator = DatasetSubtype.NOMENCLATOR_AUTHORITY == d.getSubtype();
    // we ignore nomenclators superpower for now, see https://github.com/gbif/checklistbank/issues/139
    src.nomenclator = false;
    if (sourceConfig.rank != null) {
      src.ignoreRanksAbove = sourceConfig.rank;
    }
    if (sourceConfig.homonyms) {
      src.supragenericHomonymSource = true;
      LOG.info("Allow suprageneric homonyms for nub source {}", d.getTitle());
    }
    if (sourceConfig.OTU) {
      src.includeOTUs = true;
      LOG.info("Allow OTU names for nub source {}", d.getTitle());
    }
    if (sourceConfig.scope != null) {
      src.scope = sourceConfig.scope;
    }
    return src;
  }

  private List<NubSource> loadSources() {
    LOG.info("Loading backbone sources from {} config entries", cfg.sources.size());

    Set<UUID> keys = new HashSet<>();
    List<NubSource> sources = new ArrayList<>();

    for (NubSourceConfig sd : cfg.sources) {
      if (keys.contains(sd.key)) {
        LOG.warn("Duplicate source {} skipped", sd.key);
        continue;
      }
      keys.add(sd.key);
      Dataset d = datasetService.get(sd.key);
      if (d != null) {
        sources.add(buildSource(d, cfg, sd));

      } else {
        // try if its an organization
        Organization org = organizationService.get(sd.key);
        if (org != null) {
          int counter = 0;
          for (Dataset d2 : Iterables.publishedDatasets(org.getKey(), DatasetType.CHECKLIST, organizationService)) {
            if (!keys.contains(d2.getKey())) {
              sources.add(buildSource(d2, cfg, sd));
              counter++;
            }
          }
          LOG.info("Found {} nub sources published by organization {} {}", counter, org.getKey(), org.getTitle());
        } else {
          // try an installation
          Installation inst = installationService.get(sd.key);
          if (inst != null) {
            int counter = 0;
            for (Dataset d2 : Iterables.hostedDatasets(inst.getKey(), DatasetType.CHECKLIST, installationService)) {
              if (!keys.contains(d2.getKey())) {
                sources.add(buildSource(d2, cfg, sd));
                counter++;
              }
            }
            LOG.info("Found {} nub sources hosted by installation {} {}", counter, inst.getKey(), inst.getTitle());

          } else {
            LOG.warn("Unknown nub source {}. Ignore", sd.key);
          }
        }
      }
    }

    return sources;
  }

}
