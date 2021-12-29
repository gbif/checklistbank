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
package org.gbif.checklistbank.index.backfill;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.checklistbank.index.NameUsageDocConverter;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.service.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.service.VernacularNameServiceMyBatis;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Executable job that creates a list of {@link SolrInputDocument} using a list of {@link NameUsage} objects.
 */
public class NameUsageIndexingJob implements Callable<Integer> {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final int batchSize = 100;

  /**
   * SolrServer instance.
   */
  private final SolrClient solrClient;

  /**
   * Minimum usage key, inclusive, to process.
   */
  private final int startKey;

  /**
   * Maximum usage key, inclusive, to process.
   */
  private final int endKey;

  /**
   * Service layer.
   */
  private final UsageService nameUsageService;
  private final VernacularNameServiceMyBatis vernacularNameService;
  private final DescriptionServiceMyBatis descriptionService;
  private final DistributionServiceMyBatis distributionService;
  private final SpeciesProfileServiceMyBatis speciesProfileService;

  private StopWatch stopWatch = new StopWatch();

  /**
   * {@link NameUsage}/{@link SolrInputDocument} converter.
   */
  private final NameUsageDocConverter solrDocumentConverter;

  /**
   * Default constructor.
   */
  public NameUsageIndexingJob(final SolrClient solrClient, final UsageService nameUsageService, final int startKey,
    final int endKey, final NameUsageDocConverter solrDocumentConverter,
    final VernacularNameServiceMyBatis vernacularNameService, final DescriptionServiceMyBatis descriptionService,
    final DistributionServiceMyBatis distributionService, final SpeciesProfileServiceMyBatis speciesProfileService) {
    this.nameUsageService = nameUsageService;
    this.vernacularNameService = vernacularNameService;
    this.descriptionService = descriptionService;
    this.distributionService = distributionService;
    this.speciesProfileService = speciesProfileService;
    this.startKey = startKey;
    this.endKey = endKey;
    this.solrDocumentConverter = solrDocumentConverter;
    this.solrClient = solrClient;
  }

  /**
   * Iterates over the assigned {@link NameUsage} objects to insert the corresponding {@link SolrInputDocument}
   * objects.
   *
   * @return the total number of documents added by this Thread.
   */
  @Override
  public Integer call() throws Exception {
    // Timing information initialization
    stopWatch.start();
    log.info("Adding usages from id {} to {}", startKey, endKey);
    int docCount = 0;
    // Get all usages
    List<NameUsage> usages = nameUsageService.listRange(startKey, endKey);
    // get all component maps into memory first
    Map<Integer, List<VernacularName>> vernacularNameMap = vernacularNameService.listRange(startKey, endKey);

    Map<Integer, List<Description>> descriptionMap = descriptionService.listRange(startKey, endKey);

    Map<Integer, List<Distribution>> distributionMap = distributionService.listRange(startKey, endKey);

    Map<Integer, List<SpeciesProfile>> speciesProfileMap = speciesProfileService.listRange(startKey, endKey);

    // now we're ready to build the solr indices quicky!
    for (Iterable<NameUsage> batch : Iterables.partition(usages, batchSize)) {
      final List<SolrInputDocument> docs = Lists.newArrayList();
      try {
        for (NameUsage usage : batch) {
          if (usage==null) {
            log.warn("Unexpected null usage found in range {}-{}, docCount={}", startKey, endKey, docCount);
            continue;
          }
          UsageExtensions ext = new UsageExtensions();
          ext.speciesProfiles = speciesProfileMap.get(usage.getKey());
          ext.vernacularNames = vernacularNameMap.get(usage.getKey());
          ext.descriptions = descriptionMap.get(usage.getKey());
          ext.distributions = distributionMap.get(usage.getKey());

          List<Integer> parents = nameUsageService.listParents(usage.getKey());

          docs.add(solrDocumentConverter.toDoc(usage, parents, ext));
          docCount++;
        }
        solrClient.add(docs);
        NameUsageBatchProcessor.counter.addAndGet(docs.size());

      } catch (Exception e) {
        log.error("Error indexing document for usage batch", e);
      }
    }

    // job finished notice
    stopWatch.stop();
    log.info("Finished indexing of usages in range {}-{}. Total time: {}", startKey, endKey, stopWatch.toString());

    return docCount;
  }

}
