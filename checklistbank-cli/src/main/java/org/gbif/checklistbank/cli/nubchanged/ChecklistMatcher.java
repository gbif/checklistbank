package org.gbif.checklistbank.cli.nubchanged;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.service.checklistbank.NameUsageService;

import java.util.Map;
import java.util.UUID;

import com.beust.jcommander.internal.Maps;
import com.yammer.metrics.Meter;
import com.yammer.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ChecklistMatcher implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ChecklistMatcher.class);

  private Map<NameUsageMatch.MatchType, Integer> metrics = Maps.newHashMap();
  private final Meter matchMeter;
  private final NameUsageService usageService;
  private final NameUsageMatchingService matchingService;
  private final UUID datasetKey;

  public ChecklistMatcher(NubChangedConfiguration cfg, UUID datasetKey, MetricRegistry registry,
    NameUsageService usageService, NameUsageMatchingService matchingService) {
    this.datasetKey = datasetKey;
    this.usageService = usageService;
    this.matchingService = matchingService;
    this.matchMeter = registry.getMeters().get(NubChangedService.MATCH_METER);
  }

  public void run() {
    LOG.info("Start matching checklist {}", datasetKey);
    PagingRequest req = new PagingRequest(0, 1000);
    PagingResponse<NameUsage> resp = usageService.list(null, datasetKey, null, req);
    match(resp);
    while (!resp.isEndOfRecords()) {
      req.nextPage();
      resp = usageService.list(null, datasetKey, null, req);
      match(resp);
    }
    LOG.info("Matching of {} finished. Metrics={}", datasetKey, metrics);
  }

  private void match(PagingResponse<NameUsage> resp) {
    for (NameUsage u : resp.getResults()) {
      NameUsageMatch match = matchingService.match(u.getScientificName(), u.getRank(), u, true, false);
      incCounter(match.getMatchType());
      matchMeter.mark();
    }
  }

  private void incCounter(NameUsageMatch.MatchType matchType) {
    if (metrics.containsKey(matchType)) {
      metrics.put(matchType, metrics.get(matchType)+1);
    } else {
      metrics.put(matchType, 1);
    }
  }

  public Map<NameUsageMatch.MatchType, Integer> getMetrics() {
    return metrics;
  }
}
