package org.gbif.checklistbank.index;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.gbif.api.model.checklistbank.*;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.service.mybatis.DescriptionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.DistributionServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.SpeciesProfileServiceMyBatis;
import org.gbif.checklistbank.service.mybatis.VernacularNameServiceMyBatis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Executable job that creates a list of {@link SolrInputDocument} using a list of {@link NameUsage} objects.
 */
public class NameUsageIndexingJob implements Callable<Integer> {

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * SolrServer instance.
   */
  private final SolrServer indexWriter;

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
  public NameUsageIndexingJob(final SolrServer solr, final UsageService nameUsageService, final int startKey,
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
    this.indexWriter = solr;
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
    for (NameUsage usage : usages) {
      if (usage==null) {
          log.warn("Unexpected numm usage found in range {}-{}, docCount={}", startKey, endKey, docCount);
          continue;
      }
      try {
        indexWriter.add(solrDocumentConverter.toObject(usage, vernacularNameMap.get(usage.getKey()),
        descriptionMap.get(usage.getKey()),distributionMap.get(usage.getKey()), speciesProfileMap.get(usage.getKey())));

      } catch (Exception e) {
        log.error("Error indexing document for usage {}", usage.getKey(), e);
      }
      docCount++;
      NameUsageIndexer.counter.incrementAndGet();
    }
    // job finished notice
    stopWatch.stop();
    log.info("Finished indexing of usages in range {}-{}. Total time: {}",
      new Object[] {startKey, endKey, stopWatch.toString()});

    return docCount;
  }

}
