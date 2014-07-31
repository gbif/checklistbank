package org.gbif.checklistbank.index;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.api.service.checklistbank.DistributionService;
import org.gbif.api.service.checklistbank.SpeciesProfileService;
import org.gbif.api.service.checklistbank.VernacularNameService;
import org.gbif.checklistbank.service.UsageService;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.google.inject.Inject;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

/**
 * Service that updates a solr checklistbank index in real time.
 */
public class NameUsageIndexServiceSolr implements NameUsageIndexService {

  private final NameUsageDocConverter converter = new NameUsageDocConverter();
  private final SolrServer solr;
  private final int commitWithinMs = 60*1000;
  private final UsageService usageService;
  private final VernacularNameService vernacularNameService;
  private final DescriptionService descriptionService;
  private final DistributionService distributionService;
  private final SpeciesProfileService speciesProfileService;
  // consider only some extension records at most
  private final PagingRequest page = new PagingRequest(0, 500);

  @Inject
  public NameUsageIndexServiceSolr(
    SolrServer solr,
    UsageService usageService,
    VernacularNameService vernacularNameService,
    DescriptionService descriptionService,
    DistributionService distributionService,
    SpeciesProfileService speciesProfileService
  ) {
    this.solr = solr;
    this.usageService = usageService;
    this.vernacularNameService = vernacularNameService;
    this.descriptionService = descriptionService;
    this.distributionService = distributionService;
    this.speciesProfileService = speciesProfileService;
  }

  @Override
  public void delete(int usageKey) {
    try {
      solr.deleteById(String.valueOf(usageKey), commitWithinMs);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void delete(UUID datasetKey) {
    try {
      // TODO: implement the delete query
      solr.deleteByQuery(null, commitWithinMs);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  //TODO: do this asynchroneously
  @Override
  public void insertOrUpdate(Collection<Integer> usageKeys) {
    for (Integer key : usageKeys) {
      if (key != null) {
        // we use the list service cause its more effective leaving out fields that we do not index in solr
        // TODO: consider getting several usages in one go if their keys are close to each other?
        List<NameUsage> range = usageService.listRange(key, key);
        if (!range.isEmpty()) {
          NameUsage u = range.get(0);
          insertOrUpdate(u,
                         vernacularNameService.listByUsage(key, page).getResults(),
                         descriptionService.listByUsage(key, page).getResults(),
                         distributionService.listByUsage(key, page).getResults(),
                         speciesProfileService.listByUsage(key, page).getResults()
          );
        }
      }
    }
  }

  @Override
  public void insertOrUpdate(
    NameUsage usage,
    List<VernacularName> vernaculars,
    List<Description> descriptions,
    List<Distribution> distributions,
    List<SpeciesProfile> profiles
  ) {
    SolrInputDocument doc = converter.toObject(usage, vernaculars, descriptions, distributions, profiles);
    try {
      solr.add(doc, commitWithinMs);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
