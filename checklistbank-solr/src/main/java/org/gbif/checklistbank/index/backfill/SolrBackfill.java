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

import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.index.NameUsageDocConverter;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.service.*;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Checklist Bank multithreaded name usage solr indexer. This class creates a pool of configurable
 * <i>threads</i> that concurrently execute a number of jobs each processing a configurable number
 * of name usages (<i>batchSize</i>) using a configurable number of concurrent lucene
 * <i>writers</i>. The indexer makes direct use of the mybatis layer and requires a checklist bank
 * datasource to be configured.
 */
public class SolrBackfill extends NameUsageBatchProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(SolrBackfill.class);

  // other injected instances
  private NameUsageDocConverter solrDocumentConverter;
  private final SolrClient solr;

  public SolrBackfill(
      SolrClient solr,
      Integer threads,
      Integer batchSize,
      Integer logInterval,
      UsageService nameUsageService,
      NameUsageDocConverter solrDocumentConverter,
      VernacularNameService vernacularNameService,
      DescriptionService descriptionService,
      DistributionService distributionService,
      SpeciesProfileService speciesProfileService) {

    super(
        threads,
        batchSize,
        logInterval,
        nameUsageService,
        vernacularNameService,
        descriptionService,
        distributionService,
        speciesProfileService);
    this.solrDocumentConverter = solrDocumentConverter;
    // final solr
    this.solr = solr;
  }

  @Override
  protected void shutdownService(int tasksCount) {
    super.shutdownService(tasksCount);
    // commit solr
    try {
      solr.commit();
      LOG.info("Solr server committed. Indexing completed!");
    } catch (SolrServerException | IOException e) {
      LOG.error("Error committing solr", e);
    }
  }

  @Override
  protected Callable<Integer> newBatchJob(
      int startKey,
      int endKey,
      UsageService nameUsageService,
      VernacularNameServiceMyBatis vernacularNameService,
      DescriptionServiceMyBatis descriptionService,
      DistributionServiceMyBatis distributionService,
      SpeciesProfileServiceMyBatis speciesProfileService) {
    return new NameUsageIndexingJob(
        solr,
        nameUsageService,
        startKey,
        endKey,
        solrDocumentConverter,
        vernacularNameService,
        descriptionService,
        distributionService,
        speciesProfileService);
  }
}
